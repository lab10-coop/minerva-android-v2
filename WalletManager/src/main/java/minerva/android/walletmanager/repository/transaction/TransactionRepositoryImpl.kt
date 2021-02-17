package minerva.android.walletmanager.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.TokenBalance
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.PendingTransactionToPendingAccountMapper
import minerva.android.walletmanager.model.mappers.TransactionCostPayloadToTransactionCost
import minerva.android.walletmanager.model.mappers.TransactionToTransactionPayloadMapper
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MarketUtils
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class TransactionRepositoryImpl(
    private val blockchainRepository: BlockchainRegularAccountRepository,
    private val walletConfigManager: WalletConfigManager,
    private val cryptoApi: CryptoApi,
    private val localStorage: LocalStorage,
    private val webSocketRepository: WebSocketRepository,
    private val tokenManager: TokenManager
) : TransactionRepository {
    override val masterSeed: MasterSeed
        get() = walletConfigManager.masterSeed

    override fun refreshBalances(): Single<HashMap<String, Balance>> {
        walletConfigManager.getWalletConfig()?.accounts?.filter { accountsFilter(it) }?.let { accounts ->
            return blockchainRepository.refreshBalances(getAddresses(accounts))
                .zipWith(
                    cryptoApi.getMarkets(MarketUtils.getMarketsIds(accounts), EUR_CURRENCY).onErrorReturnItem(Markets())
                )
                .map { (cryptoBalances, markets) -> MarketUtils.calculateFiatBalances(cryptoBalances, accounts, markets) }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun accountsFilter(it: Account) =
        refreshBalanceFilter(it) && it.network.testNet == !localStorage.areMainNetsEnabled

    private fun getAddresses(accounts: List<Account>): List<Pair<String, String>> =
        accounts.map { it.network.short to it.address }

    private fun refreshBalanceFilter(it: Account) = !it.isDeleted && !it.isPending

    override fun refreshTokenBalance(): Single<Map<String, List<AccountToken>>> =
        walletConfigManager.getWalletConfig()?.accounts?.let { accounts ->
            return Observable.range(START, accounts.size)
                .filter { position -> accountsFilter(accounts[position]) && accounts[position].network.isAvailable() }
                .flatMapSingle { position ->
                    refreshTokensBalance(accounts[position])
                }
                .toList()
                .map { it.associate { it.second to tokenManager.mapToAccountTokensList(it.first, it.third) } }
                .map { tokenManager.updateTokensFromLocalStorage(it) }
                .flatMap { localCheck ->
                    tokenManager.updateTokens(localCheck).onErrorReturn {
                        Timber.e(it.message)
                        localCheck.second
                    }
                }
                .flatMap {
                    tokenManager.saveTokens(it).onErrorComplete {
                        Timber.e(it.message)
                        true
                    }.andThen(Single.just(it))
                }
        } ?: Single.error(NotInitializedWalletConfigThrowable())

    override fun transferNativeCoin(network: String, accountIndex: Int, transaction: Transaction): Completable =
        blockchainRepository.transferNativeCoin(
            network,
            accountIndex,
            TransactionToTransactionPayloadMapper.map(transaction)
        )
            .map { pendingTx ->
                /*Subscription to web sockets doesn't work with http rpc, hence pending tsx are not saved*/
                if (NetworkManager.getNetwork(pendingTx.network).wsRpc.isNotEmpty()) {
                    localStorage.savePendingAccount(PendingTransactionToPendingAccountMapper.map(pendingTx))
                }
            }
            .flatMap { blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty } }
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun getTransactions(): Single<List<PendingAccount>> =
        blockchainRepository.getTransactions(getTxHashes())
            .map { getPendingAccountsWithBlockHashes(it) }

    override fun getTransactionCosts(
        network: String,
        tokenIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal
    ): Single<TransactionCost> =
        if (NetworkManager.getNetwork(network).gasPriceOracle.isNotEmpty()) {
            cryptoApi.getGasPrice(url = NetworkManager.getNetwork(network).gasPriceOracle)
                .flatMap { gasPrice ->
                    getTxCosts(
                        network,
                        tokenIndex,
                        from,
                        to,
                        amount,
                        gasPrice.fast.divide(BigDecimal.TEN)
                    )
                }
                .onErrorResumeNext { getTxCosts(network, tokenIndex, from, to, amount, null) }

        } else {
            getTxCosts(network, tokenIndex, from, to, amount, null)
        }

    private fun getTxCosts(
        network: String,
        tokenIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal,
        gasPrice: BigDecimal?
    ) = blockchainRepository.getTransactionCosts(network, tokenIndex, from, to, amount, gasPrice)
        .map { TransactionCostPayloadToTransactionCost.map(it) }

    override fun isAddressValid(address: String): Boolean =
        blockchainRepository.isAddressValid(address)

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        blockchainRepository.run { getTransactionCostInEth(toGwei(gasPrice), BigDecimal(gasLimit)) }

    private fun getPendingAccountsWithBlockHashes(it: List<Pair<String, String?>>): MutableList<PendingAccount> {
        val pendingList = mutableListOf<PendingAccount>()
        it.forEach {
            localStorage.getPendingAccounts().forEach { tx ->
                if (it.first == tx.txHash) {
                    tx.blockHash = it.second
                    pendingList.add(tx)
                }
            }
        }
        return pendingList
    }

    private fun getTxHashes(): MutableList<Pair<String, String>> {
        val txHashes = mutableListOf<Pair<String, String>>()
        localStorage.getPendingAccounts().forEach {
            txHashes.add(Pair(it.network, it.txHash))
        }
        return txHashes
    }

    override fun shouldOpenNewWssConnection(accountIndex: Int): Boolean {
        val network = getPendingAccount(accountIndex).network
        return when {
            getPendingAccounts().size == ONE_PENDING_ACCOUNT -> isFirstAccountPending(accountIndex, network)
            getPendingAccounts().size > ONE_PENDING_ACCOUNT -> isNetworkAlreadyPending(network)
            else -> false
        }
    }

    private fun isFirstAccountPending(accountIndex: Int, network: String) =
        getPendingAccounts().find { it.index == accountIndex && it.network == network } != null

    private fun isNetworkAlreadyPending(network: String) =
        getPendingAccounts().filter { it.network == network }.size < PENDING_NETWORK_LIMIT &&
                getPendingAccounts().first().network != network


    override fun subscribeToExecutedTransactions(accountIndex: Int): Flowable<PendingAccount> {
        val pendingAccount = getPendingAccount(accountIndex)
        return webSocketRepository.subscribeToExecutedTransactions(pendingAccount.network, pendingAccount.blockNumber)
            .filter { findPendingAccount(it) != null }
            .map { findPendingAccount(it) }
    }

    private fun findPendingAccount(transaction: ExecutedTransaction): PendingAccount? =
        localStorage.getPendingAccounts()
            .find { it.txHash == transaction.txHash && it.senderAddress == transaction.senderAddress }

    override fun removePendingAccount(pendingAccount: PendingAccount) {
        localStorage.removePendingAccount(pendingAccount)
    }

    override fun getPendingAccount(accountIndex: Int): PendingAccount =
        localStorage.getPendingAccounts().find { it.index == accountIndex }.orElse { PendingAccount(Int.InvalidIndex) }

    override fun clearPendingAccounts() {
        localStorage.clearPendingAccounts()
    }

    override fun getPendingAccounts(): List<PendingAccount> =
        localStorage.getPendingAccounts()

    override fun transferERC20Token(network: String, transaction: Transaction): Completable =
        blockchainRepository.transferERC20Token(network, TransactionToTransactionPayloadMapper.map(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))
    override fun loadRecipients(): List<Recipient> = localStorage.getRecipients()
    override fun resolveENS(ensName: String): Single<String> = blockchainRepository.resolveENS(ensName)

    /**
     *
     * return statement: Single<Triple<String, String, List<Pair<String, BigDecimal>>>>
     *                   Single<Triple<Network, AccountPrivateKey, Map<ContractAddress, BalanceOnContract>>>>
     *
     */

    private fun refreshTokensBalance(account: Account): Single<Triple<String, String, Map<String, TokenBalance>>> =
        cryptoApi.getTokenBalance(url = tokenManager.getTokensApiURL(account))
            .map {
                val tokenMap = mutableMapOf<String, TokenBalance>()
                it.tokens.forEach { tokenBalance -> tokenMap[tokenBalance.address] = tokenBalance }
                Triple(account.network.short, account.privateKey, tokenMap)
            }

    override fun getAccount(accountIndex: Int): Account? = walletConfigManager.getAccount(accountIndex)
    override fun getAccountByAddress(address: String): Account? =
        walletConfigManager.getWalletConfig()?.accounts?.find {
            blockchainRepository.toChecksumAddress(it.address) == address
        }

    override fun getFreeATS(address: String) = blockchainRepository.getFreeATS(address)
    override fun updateTokenIcons(): Completable = tokenManager.updateTokenIcons()
    override fun getMnemonic(): String = walletConfigManager.getMnemonic()

    companion object {
        private const val ONE_PENDING_ACCOUNT = 1
        private const val PENDING_NETWORK_LIMIT = 2
        private const val START = 0
        private const val EUR_CURRENCY = "eur"
    }
}