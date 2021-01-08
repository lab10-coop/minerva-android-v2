package minerva.android.walletmanager.repository.transaction

import com.exchangemarketsprovider.api.BinanceApi
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.function.orElse
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.mappers.PendingTransactionToPendingAccountMapper
import minerva.android.walletmanager.model.mappers.TransactionCostPayloadToTransactionCost
import minerva.android.walletmanager.model.mappers.TransactionToTransactionPayloadMapper
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MarketUtils
import java.math.BigDecimal
import java.math.BigInteger

class TransactionRepositoryImpl(
    private val blockchainRepository: BlockchainRegularAccountRepository,
    private val walletConfigManager: WalletConfigManager,
    private val binanceApi: BinanceApi,
    private val localStorage: LocalStorage,
    private val webSocketRepository: WebSocketRepository,
    private val servicesApi: ServicesApi
) : TransactionRepository {

    override fun refreshBalances(): Single<HashMap<String, Balance>> {
        val exchangeRateList = listOf(Markets.ETH_EUR, Markets.POA_ETH, Markets.ETH_DAI)
        walletConfigManager.getWalletConfig()?.accounts?.filter { refreshBalanceFilter(it) }
            ?.let { values ->
                return blockchainRepository.refreshBalances(getAddresses(values))
                    .zipWith(Observable.range(START, exchangeRateList.size)
                        .filter { walletConfigManager.areMainNetworksEnabled }
                        .flatMapSingle { binanceApi.fetchExchangeRate(exchangeRateList[it]) }.toList()
                    )
                    .map { (cryptoBalances, markets) -> MarketUtils.calculateFiatBalances(cryptoBalances, values, markets) }
            }
        throw NotInitializedWalletConfigThrowable()
    }

    private fun getAddresses(accounts: List<Account>): List<Pair<String, String>> =
        accounts.map { it.network.short to it.address }

    private fun refreshBalanceFilter(it: Account) = !it.isDeleted && !it.isPending

    override fun refreshAssetBalance(): Single<Map<String, List<AccountAsset>>> {
        walletConfigManager.getWalletConfig()?.let { config ->
            return Observable.range(START, config.accounts.size)
                .filter { position -> refreshBalanceFilter(config.accounts[position]) }
                .filter { position -> config.accounts[position].network.isAvailable() }
                //TODO checking balance of tokens will be part of another task
                //.filter { position -> config.accounts[position].network.assets.isEmpty() }
                .flatMapSingle { position -> refreshAssetsBalance(config.accounts[position]) }
                .toList()
                .map { list -> list.map { it.first to NetworkManager.mapToAssets(it.second) }.toMap() }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun transferNativeCoin(network: String, accountIndex: Int, transaction: Transaction): Completable =
        blockchainRepository.transferNativeCoin(network, accountIndex, TransactionToTransactionPayloadMapper.map(transaction))
            .map { pendingTx ->
                /*Subscription to web sockets doesn't work with http rpc, hence pending tsx are not saved*/
                if (NetworkManager.getNetwork(pendingTx.network).wsRpc != String.Empty) {
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
        assetIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal
    ): Single<TransactionCost> =
        if (NetworkManager.getNetwork(network).gasPriceOracle.isNotEmpty()) {
            servicesApi.getGasPrice(url = NetworkManager.getNetwork(network).gasPriceOracle)
                .flatMap { gasPrice -> getTxCosts(network, assetIndex, from, to, amount, gasPrice.fast.divide(BigDecimal.TEN)) }
                .onErrorResumeNext { getTxCosts(network, assetIndex, from, to, amount, null) }

        } else {
            getTxCosts(network, assetIndex, from, to, amount, null)
        }

    private fun getTxCosts(
        network: String,
        assetIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal,
        gasPrice: BigDecimal?
    ) = blockchainRepository.getTransactionCosts(network, assetIndex, from, to, amount, gasPrice)
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
     * return statement: Single<Pair<String, List<Pair<String, BigDecimal>>>>
     *                   Single<Pair<ValuePrivateKey, List<ContractAddress, BalanceOnContract>>>>
     *
     */
    private fun refreshAssetsBalance(account: Account): Single<Pair<String, List<Pair<String, BigDecimal>>>> =
        Observable.range(START, account.network.assets.size)
            .flatMap {
                blockchainRepository.refreshAssetBalance(
                    account.privateKey,
                    account.network.short,
                    account.network.getAssetsAddresses()[it],
                    account.address
                )
            }
            .toList()
            .map { Pair(account.privateKey, it) }

    override fun getAccount(accountIndex: Int): Account? = walletConfigManager.getAccount(accountIndex)

    companion object {
        private const val ONE_PENDING_ACCOUNT = 1
        private const val PENDING_NETWORK_LIMIT = 2
        private const val START = 0
    }
}