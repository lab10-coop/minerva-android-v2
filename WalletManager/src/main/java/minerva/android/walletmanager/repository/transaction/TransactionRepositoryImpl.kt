package minerva.android.walletmanager.repository.transaction

import com.exchangemarketsprovider.api.BinanceApi
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketServiceProvider
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.function.orElse
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
    private val webSocketProvider: WebSocketServiceProvider
) : TransactionRepository {

    override fun refreshBalances(): Single<HashMap<String, Balance>> {
        listOf(Markets.ETH_EUR, Markets.POA_ETH).run {
            walletConfigManager.getWalletConfig()?.accounts?.filter { !it.isDeleted && !it.isPending }?.let { values ->
                return blockchainRepository.refreshBalances(MarketUtils.getAddresses(values))
                    .zipWith(Observable.range(START, this.size).flatMapSingle { binanceApi.fetchExchangeRate(this[it]) }.toList())
                    .map { MarketUtils.calculateFiatBalances(it.first, values, it.second) }
            }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun transferNativeCoin(network: String, accountIndex: Int, transaction: Transaction): Completable =
        blockchainRepository.transferNativeCoin(network, accountIndex, TransactionToTransactionPayloadMapper.map(transaction))
            .map { pendingTx -> localStorage.savePendingAccount(PendingTransactionToPendingAccountMapper.map(pendingTx)) }
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
        blockchainRepository.getTransactionCosts(network, assetIndex, from, to, amount)
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

    override fun shouldOpenNewWssConnection(accountIndex: Int): Boolean =
        getPendingAccounts().size == ONE_PENDING_ACCOUNT && getPendingAccounts().find { it.index == accountIndex } != null ||
                getPendingAccounts().size > ONE_PENDING_ACCOUNT && !isSubscribeToTheNetwork(accountIndex)

    private fun isSubscribeToTheNetwork(accountIndex: Int): Boolean {
        val network = getPendingAccount(accountIndex).network
        return getPendingAccounts().find { it.network == network } != null
    }

    override fun subscribeToExecutedTransactions(accountIndex: Int): Flowable<PendingAccount> {
        getPendingAccount(accountIndex).network.apply {
            webSocketProvider.openConnection(this)
            return webSocketProvider.subscribeToExecutedTransactions(this)
                .filter { findPendingAccount(it) != null }
                .map { findPendingAccount(it) }
        }
    }

    private fun findPendingAccount(transaction: ExecutedTransaction) =
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

    override fun refreshAssetBalance(): Single<Map<String, List<AccountAsset>>> {
        walletConfigManager.getWalletConfig()?.let { config ->
            return Observable.range(START, config.accounts.size)
                .filter { position -> !config.accounts[position].isDeleted && !config.accounts[position].isPending }
                .filter { position -> config.accounts[position].network.isAvailable() }
                .filter { position -> config.accounts[position].network.assets.isEmpty() }
                .flatMapSingle { position -> refreshAssetsBalance(config.accounts[position]) }
                .toList()
                .map { list -> list.map { it.first to NetworkManager.mapToAssets(it.second) }.toMap() }
        }
        throw NotInitializedWalletConfigThrowable()
    }

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
            .filter { it.second > NO_FUNDS }
            .toList()
            .map { Pair(account.privateKey, it) }

    override fun getAccount(accountIndex: Int): Account? = walletConfigManager.getAccount(accountIndex)

    companion object {
        private const val ONE_PENDING_ACCOUNT = 1
        private const val START = 0
        private val NO_FUNDS = BigDecimal.valueOf(0)
    }

}