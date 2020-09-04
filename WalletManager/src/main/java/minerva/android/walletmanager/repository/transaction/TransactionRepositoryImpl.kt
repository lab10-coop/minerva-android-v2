package minerva.android.walletmanager.repository.transaction

import com.exchangemarketsprovider.api.BinanceApi
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.mappers.TransactionCostPayloadToTransactionCost
import minerva.android.walletmanager.model.mappers.TransactionToTransactionPayloadMapper
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MarketUtils
import java.math.BigDecimal
import java.math.BigInteger

class TransactionRepositoryImpl(
    private val blockchainRepository: BlockchainRepository,
    private val walletConfigManager: WalletConfigManager,
    private val binanceApi: BinanceApi,
    private val localStorage: LocalStorage
) : TransactionRepository {

    private var transactionHash: String = String.Empty

    override fun refreshBalances(): Single<HashMap<String, Balance>> {
        listOf(Markets.ETH_EUR, Markets.POA_ETH).run {
            walletConfigManager.getWalletConfig()?.accounts?.filter { !it.isDeleted }?.let { values ->
                return blockchainRepository.refreshBalances(MarketUtils.getAddresses(values))
                    .zipWith(Observable.range(START, this.size).flatMapSingle { binanceApi.fetchExchangeRate(this[it]) }.toList())
                    .map { MarketUtils.calculateFiatBalances(it.first, values, it.second) }
            }
        }
        return Single.error(NotInitializedWalletConfigThrowable())
    }

    override fun transferNativeCoin(network: String, transaction: Transaction): Single<String> =
        blockchainRepository.transferNativeCoin(network, TransactionToTransactionPayloadMapper.map(transaction))
            .doOnSuccess {
                blockchainRepository.reverseResolveENS(transaction.receiverKey)
                    .onErrorReturn { String.Empty }
                    .map { saveRecipient(it, transaction.receiverKey) }
            }

    override fun transferERC20Token(network: String, transaction: Transaction): Completable =
        blockchainRepository.transferERC20Token(network, TransactionToTransactionPayloadMapper.map(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))

    override fun loadRecipients(): List<Recipient> = localStorage.loadRecipients()

    override fun resolveENS(ensName: String): Single<String> = blockchainRepository.resolveENS(ensName)

    override fun getTransferCosts(network: String, assetIndex: Int): TransactionCost {
        val operation = if (assetIndex == Int.InvalidIndex) Operation.TRANSFER_NATIVE else Operation.TRANSFER_ERC20
        return TransactionCostPayloadToTransactionCost.map(blockchainRepository.getTransactionCosts(network, assetIndex, operation))
    }

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        blockchainRepository.calculateTransactionCost(gasPrice, gasLimit)

    override fun refreshAssetBalance(): Single<Map<String, List<AccountAsset>>> {
        walletConfigManager.getWalletConfig()?.accounts?.let { accounts ->
            return Observable.range(START, accounts.size)
                .filter { position -> !accounts[position].isDeleted }
                .filter { position -> NetworkManager.isAvailable(accounts[position].network) }
                .flatMapSingle { position -> refreshAssetsBalance(accounts[position]) }
                .toList()
                .map { list -> list.map { it.first to NetworkManager.mapToAssets(it.second) }.toMap() }
        }
        return Single.error(NotInitializedWalletConfigThrowable())
    }

    /**
     *
     * return statement: Single<Pair<String, List<Pair<String, BigDecimal>>>>
     *                   Single<Pair<ValuePrivateKey, List<ContractAddress, BalanceOnContract>>>>
     *
     */
    private fun refreshAssetsBalance(
        account: Account
    ): Single<Pair<String, List<Pair<String, BigDecimal>>>> =
        Observable.range(START, NetworkManager.getAssetsAddresses(account.network).size)
            .flatMap {
                blockchainRepository.refreshAssetBalance(
                    account.privateKey,
                    account.network,
                    NetworkManager.getAssetsAddresses(account.network)[it],
                    account.address
                )
            }
            .filter { it.second > NO_FUNDS }
            .toList()
            .map { Pair(account.privateKey, it) }

    override fun getAccount(valueIndex: Int, assetIndex: Int): Account? = walletConfigManager.getAccount(valueIndex, assetIndex)

    override fun currentTransactionHash(transactionHash: String) {
        this.transactionHash = transactionHash
    }

    companion object {
        private const val START = 0
        private val NO_FUNDS = BigDecimal.valueOf(0)
    }
}