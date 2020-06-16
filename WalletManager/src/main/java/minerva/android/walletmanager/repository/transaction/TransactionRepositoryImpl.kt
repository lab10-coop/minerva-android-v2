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
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.mappers.mapTransactionCostPayloadToTransactionCost
import minerva.android.walletmanager.model.mappers.mapTransactionToTransactionPayload
import minerva.android.walletmanager.smartContract.assets.AssetManager
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
            walletConfigManager.getWalletConfig()?.values?.filter { !it.isDeleted }?.let { values ->
                return blockchainRepository.refreshBalances(MarketUtils.getAddresses(values))
                    .zipWith(Observable.range(START, this.size).flatMapSingle { binanceApi.fetchExchangeRate(this[it]) }.toList())
                    .map { MarketUtils.calculateFiatBalances(it.first, values, it.second) }
            }
        }
        return Single.error(Throwable("Wallet Config was not initialized"))
    }

    override fun transferNativeCoin(network: String, transaction: Transaction): Single<String> {
        return blockchainRepository.transferNativeCoin(network, mapTransactionToTransactionPayload(transaction))
            .doOnSuccess {
                blockchainRepository.reverseResolveENS(transaction.receiverKey)
                    .onErrorReturn { String.Empty }
                    .map { saveRecipient(it, transaction.receiverKey) }
            }
    }

    override fun transferERC20Token(network: String, transaction: Transaction): Completable =
        blockchainRepository.transferERC20Token(network, mapTransactionToTransactionPayload(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))

    override fun loadRecipients(): List<Recipient> = localStorage.loadRecipients()

    override fun resolveENS(ensName: String): Single<String> = blockchainRepository.resolveENS(ensName)

    override fun getTransferCosts(network: String, assetIndex: Int): TransactionCost {
        val operation = if (assetIndex == Int.InvalidIndex) Operation.TRANSFER_NATIVE else Operation.TRANSFER_ERC20
        return mapTransactionCostPayloadToTransactionCost(blockchainRepository.getTransactionCosts(network, assetIndex, operation))
    }

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        blockchainRepository.calculateTransactionCost(gasPrice, gasLimit)

    override fun refreshAssetBalance(): Single<Map<String, List<Asset>>> {
        walletConfigManager.getWalletConfig()?.values?.let { values ->
            return Observable.range(START, values.size)
                .filter { position -> !values[position].isDeleted }
                //TODO filter should be removed when all test net will be implemented
                .filter { position -> Network.fromString(values[position].network).run { this == Network.ETHEREUM || this == Network.ARTIS } }
                .flatMapSingle { position ->
                    refreshAssetsBalance(values[position], AssetManager.getAssetAddresses(Network.fromString(values[position].network)))
                }
                .toList()
                .map { list -> list.map { it.first to AssetManager.mapToAssets(it.second) }.toMap() }
        }
        return Single.error(Throwable("Wallet Config was not initialized"))
    }

    /**
     *
     * return statement: Single<Pair<String, List<Pair<String, BigDecimal>>>>
     *                   Single<Pair<ValuePrivateKey, List<ContractAddress, BalanceOnContract>>>>
     *
     */
    private fun refreshAssetsBalance(
        value: Value,
        addresses: Pair<String, List<String>>
    ): Single<Pair<String, List<Pair<String, BigDecimal>>>> =
        Observable.range(START, addresses.second.size)
            .flatMap { blockchainRepository.refreshAssetBalance(value.privateKey, addresses.first, addresses.second[it], value.address) }
            .filter { it.second > NO_FUNDS }
            .toList()
            .map { Pair(value.privateKey, it) }

    override fun getValue(valueIndex: Int, assetIndex: Int): Value? = walletConfigManager.getValue(valueIndex, assetIndex)

    override fun currentTransactionHash(transactionHash: String) {
        this.transactionHash = transactionHash
    }

    companion object {
        private const val START = 0
        private val NO_FUNDS = BigDecimal.valueOf(0)
    }
}