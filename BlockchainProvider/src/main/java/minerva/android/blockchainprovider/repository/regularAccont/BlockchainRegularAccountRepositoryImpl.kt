package minerva.android.blockchainprovider.repository.regularAccont

import io.reactivex.*
import io.reactivex.rxkotlin.zipWith
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.PendingTransaction
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.blockchainprovider.provider.Web3jProvider
import minerva.android.blockchainprovider.smartContracts.ERC20
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.map.value
import org.web3j.abi.Utils
import org.web3j.crypto.*
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthLog
import org.web3j.protocol.core.methods.response.NetVersion
import org.web3j.tx.RawTransactionManager
import org.web3j.utils.Convert
import org.web3j.utils.Convert.fromWei
import org.web3j.utils.Convert.toWei
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.Pair

class BlockchainRegularAccountRepositoryImpl(
    private val web3j: Map<String, Web3j>,
    private val gasPrice: Map<String, BigInteger>,
    private val ensResolver: EnsResolver
) : BlockchainRegularAccountRepository {

    override fun getTransactions(pendingHashes: List<Pair<String, String>>): Single<List<Pair<String, String?>>> =
        Observable.range(START, pendingHashes.size)
            .flatMapSingle { position ->
                getTransaction(pendingHashes[position].first, pendingHashes[position].second)
            }
            .toList()

    private fun getTransaction(network: String, txHash: String): Single<Pair<String, String?>> =
        web3j.value(network).ethGetTransactionByHash(txHash)
            .flowable()
            .map { ethTransaction ->
                var blockHash: String? = null
                ethTransaction.transaction.ifPresent { blockHash = it.blockHash }
                Pair(txHash, blockHash)
            }.firstOrError()

    override fun transferNativeCoin(
        network: String,
        accountIndex: Int,
        transactionPayload: TransactionPayload
    ): Single<PendingTransaction> =
        web3j.value(network).ethGetTransactionCount(transactionPayload.senderAddress, DefaultBlockParameterName.LATEST)
            .flowable()
            .zipWith(getChainId(network))
            .flatMap {
                web3j.value(network)
                    .ethSendRawTransaction(
                        getSignedTransaction(
                            it.first.transactionCount,
                            transactionPayload,
                            it.second.netVersion.toLong()
                        )
                    )
                    .flowable()
                    .flatMapSingle { response ->
                        if (response.error == null) {
                            val pendingTx = PendingTransaction(
                                accountIndex, response.transactionHash,
                                network, transactionPayload.senderAddress, String.Empty, transactionPayload.amount
                            )
                            Single.just(pendingTx)
                        } else Single.error(Throwable(response.error.message))
                    }
            }.firstOrError()

    /**
     * List arguments: first - network short name, second - wallet address (public)
     */
    override fun refreshBalances(networkAddress: List<Pair<String, String>>): Single<List<Pair<String, BigDecimal>>> =
        Observable.range(START, networkAddress.size)
            .flatMapSingle { position -> getBalance(networkAddress[position].first, networkAddress[position].second) }
            .toList()

    private fun getBalance(network: String, address: String): Single<Pair<String, BigDecimal>> =
        web3j.value(network).ethGetBalance(address, DefaultBlockParameterName.LATEST)
            .flowable()
            .map { Pair(address, fromWei(BigDecimal(it.balance), Convert.Unit.ETHER)) }
            .firstOrError()

    override fun isAddressValid(address: String): Boolean =
        WalletUtils.isValidAddress(address)

    override fun refreshAssetBalance(
        privateKey: String,
        network: String,
        contractAddress: String,
        safeAccountAddress: String
    ): Observable<Pair<String, BigDecimal>> =
        if (safeAccountAddress.isEmpty()) getERC20Balance(
            contractAddress,
            network,
            privateKey,
            Credentials.create(privateKey).address
        )
        else getERC20Balance(contractAddress, network, privateKey, safeAccountAddress)

    private fun getERC20Balance(
        contractAddress: String,
        network: String,
        privateKey: String,
        address: String
    ): Observable<Pair<String, BigDecimal>> =
        getChainId(network)
            .flatMap {
                ERC20.load(
                    contractAddress, web3j.value(network),
                    RawTransactionManager(web3j.value(network), Credentials.create(privateKey), it.netVersion.toLong()),
                    ContractGasProvider(gasPrice.value(network), Operation.TRANSFER_ERC20.gasLimit)
                )
                    .balanceOf(address).flowable()
                    .map { balance -> Pair(contractAddress, fromWei(balance.toString(), Convert.Unit.ETHER)) }

            }.toObservable()

    override fun reverseResolveENS(ensAddress: String): Single<String> {
        return Single.just(ensAddress).map { ensResolver.reverseResolve(it) }
    }

    override fun resolveENS(ensName: String): Single<String> =
        if (ensName.contains(DOT)) Single.just(ensName).map { ensResolver.resolve(it) }
        else Single.just(ensName)

    override fun transferERC20Token(network: String, payload: TransactionPayload): Completable =
        web3j.value(network).netVersion().flowable()
            .flatMapCompletable {
                Credentials.create(payload.privateKey).run {
                    ERC20.load(
                        payload.contractAddress,
                        web3j.value(network),
                        RawTransactionManager(web3j.value(network), this, it.netVersion.toLong()),
                        ContractGasProvider(toGwei(payload.gasPrice).toBigInteger(), payload.gasLimit)
                    )
                        .transfer(payload.receiverAddress, toWei(payload.amount, Convert.Unit.ETHER).toBigInteger())
                        .flowable()
                        .ignoreElements()
                }
            }

    private fun getChainId(network: String): Flowable<NetVersion> = web3j.value(network).netVersion().flowable()

    override fun getTransactionCosts(
        network: String,
        assetIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal
    ): Single<TransactionCostPayload> {
        return if (assetIndex == Int.InvalidIndex) {
            web3j.value(network).ethGetTransactionCount(from, DefaultBlockParameterName.LATEST)
                .flowable()
                .flatMap { count ->
                    web3j.value(network)
                        .ethEstimateGas(getTransaction(from, count, to, amount))
                        .flowable()
                        .flatMapSingle { handleGasLimit(network, it) }
                }
                .firstOrError()
                .timeout(5, TimeUnit.SECONDS, calculateTransactionCosts(network, Operation.TRANSFER_NATIVE.gasLimit))

        } else calculateTransactionCosts(network, Operation.TRANSFER_ERC20.gasLimit)
    }

    private fun getTransaction(from: String, count: EthGetTransactionCount, to: String, amount: BigDecimal) =
        Transaction(
            from,
            count.transactionCount,
            BigInteger.ZERO,
            BigInteger.ZERO,
            to,
            toWei(amount, Convert.Unit.ETHER).toBigInteger(),
            String.Empty
        )

    private fun handleGasLimit(network: String, it: EthEstimateGas): Single<TransactionCostPayload> {
        val gasLimit = it.error?.let { Operation.TRANSFER_NATIVE.gasLimit } ?: increaseGasLimitByTenPercent(it.amountUsed)
        return calculateTransactionCosts(network, gasLimit)
    }

    private fun increaseGasLimitByTenPercent(gasLimit: BigInteger) =
        gasLimit.add(getBuffer(gasLimit))

    private fun getBuffer(gasLimit: BigInteger) = gasLimit.multiply(BigInteger.valueOf(10)).divide(BigInteger.valueOf(100))

    private fun calculateTransactionCosts(network: String, gasLimit: BigInteger): Single<TransactionCostPayload> =
        Single.just(
            TransactionCostPayload(
                fromWei(BigDecimal(gasPrice.value(network)), Convert.Unit.GWEI),
                gasLimit,
                getTransactionCostInEth(BigDecimal(gasPrice.value(network)), BigDecimal(gasLimit))
            )
        )

    override fun toGwei(balance: BigDecimal): BigDecimal = toWei(balance, Convert.Unit.GWEI)

    override fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal): BigDecimal =
        fromWei((gasPrice * gasLimit), Convert.Unit.ETHER).setScale(SCALE, RoundingMode.HALF_EVEN)

    private fun getSignedTransaction(count: BigInteger, transactionPayload: TransactionPayload, chainId: Long): String? =
        Numeric.toHexString(
            TransactionEncoder.signMessage(
                createTransaction(count, transactionPayload), chainId,
                Credentials.create(transactionPayload.privateKey)
            )
        )

    private fun createTransaction(count: BigInteger, transactionPayload: TransactionPayload): RawTransaction? =
        transactionPayload.run {
            return RawTransaction.createEtherTransaction(
                count,
                toWei(gasPrice, Convert.Unit.GWEI).toBigInteger(),
                gasLimit,
                receiverAddress,
                toWei(amount, Convert.Unit.ETHER).toBigInteger()
            )
        }

    companion object {
        private const val START = 0
        private const val SCALE = 8
        private const val DOT = "."
    }
}
