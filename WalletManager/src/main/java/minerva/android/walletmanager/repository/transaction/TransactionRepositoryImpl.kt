package minerva.android.walletmanager.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.GasPrices
import minerva.android.apiProvider.model.MarketIds
import minerva.android.apiProvider.model.Markets
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
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.mappers.PendingTransactionToPendingAccountMapper
import minerva.android.walletmanager.model.mappers.TransactionCostPayloadToTransactionCost
import minerva.android.walletmanager.model.mappers.TransactionToTransactionPayloadMapper
import minerva.android.walletmanager.model.mappers.TxCostPayloadToTxCostDataMapper
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.*
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MarketUtils
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

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
                .zipWith(getRate(MarketUtils.getMarketsIds(accounts)).onErrorReturnItem(Markets()))
                .map { (cryptoBalances, markets) -> MarketUtils.calculateFiatBalances(cryptoBalances, accounts, markets) }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun accountsFilter(it: Account) =
        refreshBalanceFilter(it) && it.network.testNet == !localStorage.areMainNetsEnabled

    private fun getAddresses(accounts: List<Account>): List<Pair<Int, String>> =
        accounts.map { it.network.chainId to it.address }

    private fun refreshBalanceFilter(it: Account) = !it.isDeleted && !it.isPending

    override fun refreshTokenBalance(): Single<Map<String, List<AccountToken>>> =
        getActiveAccounts().let { accounts ->
            Observable.range(FIRST_INDEX, accounts.size)
                .map { accounts[it] }
                .flatMapSingle {
                    tokenManager.refreshTokenBalance(it)
                }.toList()
                .map {
                    mutableMapOf<String, List<AccountToken>>().apply {
                        it.forEach { put(it.first, it.second) }
                    }.toMap()
                }
        }

    override fun refreshTokensList(): Single<Boolean> =
        getActiveAccounts().let { accounts ->
            accounts.filter { NetworkManager.isUsingEtherScan(it.chainId) }.let { etherscanAccounts ->
                accounts.filter { !NetworkManager.isUsingEtherScan(it.chainId) }.let { notEtherscanAccounts ->
                    downloadTokensListWithBuffer(etherscanAccounts)
                        .zipWith(downloadTokensList(notEtherscanAccounts))
                        .map { (etherscanTokens, notEtherscanTokens) -> etherscanTokens + notEtherscanTokens }
                        .map { tokenManager.sortTokensByChainId(it) }
                        .map { tokenManager.mergeWithLocalTokensList(it) }
                        .flatMap { (shouldBeUpdated, accountTokens) ->
                            tokenManager.updateTokenIcons(
                                shouldBeUpdated,
                                accountTokens
                            )
                        }
                        .flatMap { (shouldBeSaved, automaticTokenUpdateMap) ->
                            tokenManager.saveTokens(shouldBeSaved, automaticTokenUpdateMap)
                        }
                }
            }
        }

    private fun getActiveAccounts(): List<Account> =
        walletConfigManager.getWalletConfig()?.accounts?.filter { account ->
            accountsFilter(account) && account.network.isAvailable()
        } ?: throw NotInitializedWalletConfigThrowable()

    private fun downloadTokensListWithBuffer(accounts: List<Account>):
            Single<List<ERC20Token>> =
        Observable.range(FIRST_INDEX, accounts.size)
            .map { accounts[it] }
            .buffer(ETHERSCAN_REQUEST_TIMESPAN, TimeUnit.SECONDS, ETHERSCAN_REQUEST_PACKAGE)
            .flatMapSingle { downloadTokensList(it) }
            .toList()
            .map {
                mutableListOf<ERC20Token>().apply {
                    it.forEach { addAll(it) }
                }
            }

    private fun downloadTokensList(accounts: List<Account>): Single<List<ERC20Token>> =
        Observable.range(FIRST_INDEX, accounts.size)
            .flatMapSingle { position -> tokenManager.downloadTokensList(accounts[position]) }
            .toList()
            .map {
                mutableListOf<ERC20Token>().apply {
                    it.forEach { addAll(it) }
                }
            }

    override fun transferNativeCoin(chainId: Int, accountIndex: Int, transaction: Transaction): Completable =
        blockchainRepository.transferNativeCoin(
            chainId,
            accountIndex,
            TransactionToTransactionPayloadMapper.map(transaction)
        ).map { pendingTx ->
            /*Subscription to web sockets doesn't work with http rpc, hence pending tsx are not saved*/
            if (NetworkManager.getNetwork(pendingTx.chainId).wsRpc.isNotEmpty()) {
                localStorage.savePendingAccount(PendingTransactionToPendingAccountMapper.map(pendingTx))
            }
        }.flatMap { blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty } }
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun getTransactions(): Single<List<PendingAccount>> =
        blockchainRepository.getTransactions(getTxHashes())
            .map { getPendingAccountsWithBlockHashes(it) }

    override fun getEurRate(chainId: Int): Single<Double> =
        when (chainId) {
            ChainId.ETH_MAIN -> getRate(MarketIds.ETHEREUM).map { it.ethPrice?.value }
            ChainId.POA_CORE -> getRate(MarketIds.POA_NETWORK).map { it.poaPrice?.value }
            ChainId.XDAI -> getRate(MarketIds.DAI).map { it.daiPrice?.value }
            else -> Single.just(0.0)
        }

    private fun getRate(id: String): Single<Markets> = cryptoApi.getMarkets(id, EUR_CURRENCY)

    override fun toEther(value: BigDecimal): BigDecimal = blockchainRepository.toEther(value)

    override fun sendTransaction(chainId: Int, transaction: Transaction): Single<String> =
        blockchainRepository.sendWalletConnectTransaction(chainId, TransactionToTransactionPayloadMapper.map(transaction))

    override fun getTransactionCosts(txCostPayload: TxCostPayload): Single<TransactionCost> = with(txCostPayload) {
        if (shouldGetGasPriceFromApi(chainId)) {
            cryptoApi.getGasPrice(url = NetworkManager.getNetwork(chainId).gasPriceOracle)
                .flatMap { gasPrice -> getTxCosts(txCostPayload, gasPrice) }
                .onErrorResumeNext { getTxCosts(txCostPayload, null) }
        } else {
            getTxCosts(txCostPayload, null)
        }
    }

    private fun shouldGetGasPriceFromApi(chainId: Int) = NetworkManager.getNetwork(chainId).gasPriceOracle.isNotEmpty()

    private fun getTxCosts(payload: TxCostPayload, gasPrice: GasPrices?): Single<TransactionCost> =
        blockchainRepository.getTransactionCosts(TxCostPayloadToTxCostDataMapper.map(payload), gasPrice?.speed?.rapid)
            .map { txCost ->
                TransactionCostPayloadToTransactionCost.map(txCost, gasPrice, payload.chainId) {
                    blockchainRepository.fromWei(it).setScale(0, RoundingMode.HALF_EVEN)
                }
            }

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

    private fun getTxHashes(): MutableList<Pair<Int, String>> {
        val txHashes = mutableListOf<Pair<Int, String>>()
        localStorage.getPendingAccounts().forEach {
            txHashes.add(Pair(it.chainId, it.txHash))
        }
        return txHashes
    }

    override fun shouldOpenNewWssConnection(accountIndex: Int): Boolean {
        val chainId = getPendingAccount(accountIndex).chainId
        return when {
            getPendingAccounts().size == ONE_PENDING_ACCOUNT -> isFirstAccountPending(accountIndex, chainId)
            getPendingAccounts().size > ONE_PENDING_ACCOUNT -> isNetworkAlreadyPending(chainId)
            else -> false
        }
    }

    private fun isFirstAccountPending(accountIndex: Int, chainId: Int) =
        getPendingAccounts().find { it.index == accountIndex && it.chainId == chainId } != null

    private fun isNetworkAlreadyPending(chainId: Int) =
        getPendingAccounts().filter { it.chainId == chainId }.size < PENDING_NETWORK_LIMIT &&
                getPendingAccounts().first().chainId != chainId


    override fun subscribeToExecutedTransactions(accountIndex: Int): Flowable<PendingAccount> {
        val pendingAccount = getPendingAccount(accountIndex)
        return webSocketRepository.subscribeToExecutedTransactions(pendingAccount.chainId, pendingAccount.blockNumber)
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

    override fun transferERC20Token(chainId: Int, transaction: Transaction): Completable =
        blockchainRepository.transferERC20Token(chainId, TransactionToTransactionPayloadMapper.map(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))
    override fun loadRecipients(): List<Recipient> = localStorage.getRecipients()
    override fun resolveENS(ensName: String): Single<String> = blockchainRepository.resolveENS(ensName)


    override fun getAccount(accountIndex: Int): Account? = walletConfigManager.getAccount(accountIndex)
    override fun getAccountByAddress(address: String): Account? =
        walletConfigManager.getWalletConfig()?.accounts?.find {
            blockchainRepository.toChecksumAddress(it.address) == address
        }

    override fun getFreeATS(address: String) = blockchainRepository.getFreeATS(address)
    override fun updateTokenIcons(): Completable = tokenManager.updateTokenIcons()

    companion object {
        private const val ONE_PENDING_ACCOUNT = 1
        private const val PENDING_NETWORK_LIMIT = 2
        private const val FIRST_INDEX = 0
        private const val EUR_CURRENCY = "eur"
        private const val ETHERSCAN_REQUEST_TIMESPAN = 1L
        private const val ETHERSCAN_REQUEST_PACKAGE = 5
    }
}