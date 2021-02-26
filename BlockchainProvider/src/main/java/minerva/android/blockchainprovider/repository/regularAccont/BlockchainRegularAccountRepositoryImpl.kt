package minerva.android.blockchainprovider.repository.regularAccont

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.PendingTransaction
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.provider.ContractGasProvider
import minerva.android.blockchainprovider.repository.freeToken.FreeTokenRepository
import minerva.android.blockchainprovider.smartContracts.ERC20
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.map.value
import org.web3j.crypto.*
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.NetVersion
import org.web3j.tx.RawTransactionManager
import org.web3j.utils.Convert
import org.web3j.utils.Convert.fromWei
import org.web3j.utils.Convert.toWei
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Pair

class BlockchainRegularAccountRepositoryImpl(
    private val web3j: Map<Int, Web3j>,
    private val gasPrices: Map<Int, BigInteger>,
    private val ensResolver: EnsResolver,
    private val freeTokenRepository: FreeTokenRepository
) : BlockchainRegularAccountRepository {

    override fun getTransactions(pendingHashes: List<Pair<Int, String>>): Single<List<Pair<String, String?>>> =
        Observable.range(START, pendingHashes.size)
            .flatMapSingle { position ->
                getTransaction(pendingHashes[position].first, pendingHashes[position].second)
            }
            .toList()

    private fun getTransaction(chainId: Int, txHash: String): Single<Pair<String, String?>> =
        web3j.value(chainId).ethGetTransactionByHash(txHash)
            .flowable()
            .map { ethTransaction ->
                var blockHash: String? = null
                ethTransaction.transaction.ifPresent { blockHash = it.blockHash }
                Pair(txHash, blockHash)
            }.firstOrError()

    override fun transferNativeCoin(
        chainId: Int,
        accountIndex: Int,
        transactionPayload: TransactionPayload
    ): Single<PendingTransaction> =
        web3j.value(chainId).ethGetTransactionCount(transactionPayload.senderAddress, DefaultBlockParameterName.LATEST)
            .flowable()
            .zipWith(getNetVersion(chainId))
            .flatMap {
                web3j.value(chainId)
                    .ethSendRawTransaction(
                        getSignedEtherTransaction(
                            it.first.transactionCount,
                            transactionPayload,
                            it.second.netVersion.toLong()
                        )
                    )
                    .flowable()
                    .zipWith(getCurrentBlockNumber(chainId))
                    .flatMapSingle { (response, blockNumber) ->
                        if (response.error == null) {
                            val pendingTx = PendingTransaction(
                                accountIndex,
                                response.transactionHash,
                                chainId,
                                transactionPayload.senderAddress,
                                String.Empty,
                                transactionPayload.amount,
                                blockNumber.subtract(BigInteger.valueOf(BLOCK_NUMBER_OFFSET)) //get n-5 block number, where n is current block number
                            )
                            Single.just(pendingTx)
                        } else Single.error(Throwable(response.error.message))
                    }
            }.firstOrError()

    override fun sendTransaction(chainId: Int, transactionPayload: TransactionPayload): Single<String> =
        web3j.value(chainId)
            .ethGetTransactionCount(transactionPayload.senderAddress, DefaultBlockParameterName.LATEST)
            .flowable()
            .zipWith(getNetVersion(chainId))
            .flatMap {
                web3j.value(chainId)
                    .ethSendRawTransaction(
                        getSignedTransaction(
                            it.first.transactionCount,
                            transactionPayload,
                            it.second.netVersion.toLong(),
                            Numeric.hexStringToByteArray(transactionPayload.data)
                        )
                    )
                    .flowable()
                    .flatMapSingle { response ->
                        if (response.error == null) {
                            Single.just(response.transactionHash)
                        } else {
                            Single.error(Throwable(response.error.message))
                        }
                    }
            }.firstOrError()


    override fun getCurrentBlockNumber(chainId: Int): Flowable<BigInteger> =
        web3j.value(chainId)
            .ethBlockNumber()
            .flowable()
            .map { it.blockNumber }

    /**
     * List arguments: first - network short name, second - wallet address (public)
     */
    override fun refreshBalances(networkAddress: List<Pair<Int, String>>): Single<List<Pair<String, BigDecimal>>> =
        Observable.range(START, networkAddress.size)
            .flatMapSingle { position ->
                getBalance(
                    networkAddress[position].first,
                    networkAddress[position].second
                )
            }
            .toList()

    override fun toChecksumAddress(address: String): String = Keys.toChecksumAddress(address)

    override fun isAddressValid(address: String): Boolean =
        WalletUtils.isValidAddress(address) &&
                (Keys.toChecksumAddress(address) == address || address.toLowerCase(Locale.ROOT) == address)

    override fun getERC20TokenName(privateKey: String, chainId: Int, tokenAddress: String): Observable<String> =
        getNetVersion(chainId).flatMap {
            loadERC20(privateKey, chainId, tokenAddress, it).name().flowable()
        }.toObservable()

    override fun getERC20TokenSymbol(privateKey: String, chainId: Int, tokenAddress: String): Observable<String> =
        getNetVersion(chainId).flatMap {
            loadERC20(privateKey, chainId, tokenAddress, it).symbol().flowable()
        }.toObservable()

    override fun getERC20TokenDecimals(
        privateKey: String,
        chainId: Int,
        tokenAddress: String
    ): Observable<BigInteger> =
        getNetVersion(chainId).flatMap {
            loadERC20(privateKey, chainId, tokenAddress, it).decimals().flowable()
        }.toObservable()

    override fun getFreeATS(address: String): Completable = Completable.create {
        freeTokenRepository.getFreeATS(address).let { responseText ->
            if (responseText.startsWith(CORRECT_ATS_FREE_PREFIX)) it.onComplete()
            else it.onError(Throwable(responseText))
        }
    }

    override fun reverseResolveENS(ensAddress: String): Single<String> =
        Single.just(ensAddress).map { ensResolver.reverseResolve(it) }

    override fun resolveENS(ensName: String): Single<String> =
        if (ensName.contains(DOT)) Single.just(ensName).map { ensResolver.resolve(it) }.onErrorReturnItem(ensName)
        else Single.just(ensName)

    override fun transferERC20Token(chainId: Int, payload: TransactionPayload): Completable =
        getNetVersion(chainId).flatMapCompletable {
            loadERC20(payload.privateKey, chainId, payload.contractAddress, it)
                .transfer(payload.receiverAddress, toWei(payload.amount, Convert.Unit.ETHER).toBigInteger())
                .flowable()
                .ignoreElements()
        }

    override fun getTransactionCosts(
        chainId: Int,
        assetIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal,
        gasPrice: BigDecimal?,
        contractData: String
    ): Single<TransactionCostPayload> =
        if (assetIndex == Int.InvalidIndex) {
            web3j.value(chainId).ethGetTransactionCount(from, DefaultBlockParameterName.LATEST)
                .flowable()
                .zipWith(resolveENS(to).toFlowable())
                .flatMap { (count, address) ->
                    web3j.value(chainId).ethEstimateGas(getTransaction(from, count, address, amount, contractData))
                        .flowable()
                        .flatMapSingle { handleGasLimit(chainId, it, gasPrice) }
                }
                .firstOrError()
                .timeout(
                    TIMEOUT,
                    TimeUnit.SECONDS,
                    calculateTransactionCosts(chainId, Operation.TRANSFER_NATIVE.gasLimit, gasPrice)
                )

        } else calculateTransactionCosts(chainId, Operation.TRANSFER_ERC20.gasLimit, gasPrice)


    private fun handleGasLimit(
        chainId: Int,
        it: EthEstimateGas,
        gasPrice: BigDecimal?
    ): Single<TransactionCostPayload> {
        val gasLimit = it.error?.let { Operation.TRANSFER_NATIVE.gasLimit } ?: estimateGasLimit(it.amountUsed)
        return calculateTransactionCosts(chainId, gasLimit, gasPrice)
    }

    override fun refreshTokenBalance(
        privateKey: String,
        chainId: Int,
        contractAddress: String,
        safeAccountAddress: String
    ): Observable<Pair<String, BigDecimal>> =
        if (safeAccountAddress.isEmpty()) getERC20Balance(
            contractAddress,
            chainId,
            privateKey,
            Credentials.create(privateKey).address
        )
        else getERC20Balance(contractAddress, chainId, privateKey, safeAccountAddress)


    override fun toGwei(amount: BigDecimal): BigDecimal = toWei(amount, Convert.Unit.GWEI)
    override fun fromWei(value: BigDecimal): BigDecimal = fromWei(value, Convert.Unit.GWEI)
    override fun toEther(value: BigDecimal): BigDecimal = fromWei(value, Convert.Unit.ETHER)

    override fun fromGwei(amount: BigDecimal): BigDecimal = fromWei(amount, Convert.Unit.ETHER)

    override fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal): BigDecimal =
        toEther((gasPrice * gasLimit)).setScale(SCALE, RoundingMode.HALF_EVEN)

    private fun loadERC20(privateKey: String, chainId: Int, address: String, netVersion: NetVersion) =
        ERC20.load(
            address, web3j.value(chainId),
            RawTransactionManager(
                web3j.value(chainId),
                Credentials.create(privateKey),
                netVersion.netVersion.toLong()
            ),
            ContractGasProvider(gasPrices.value(chainId), Operation.TRANSFER_ERC20.gasLimit)
        )

    private fun getBalance(chainId: Int, address: String): Single<Pair<String, BigDecimal>> =
        web3j.value(chainId).ethGetBalance(address, DefaultBlockParameterName.LATEST)
            .flowable()
            .map { Pair(address, toEther(BigDecimal(it.balance))) }
            .firstOrError()

    private fun getERC20Balance(
        contractAddress: String,
        chainId: Int,
        privateKey: String,
        address: String
    ): Observable<Pair<String, BigDecimal>> =
        getNetVersion(chainId)
            .flatMap {
                loadERC20(privateKey, chainId, contractAddress, it).balanceOf(address).flowable()
                    .map { balance -> Pair(contractAddress, balance.toBigDecimal()) }
            }.toObservable()

    private fun getNetVersion(chainId: Int): Flowable<NetVersion> = web3j.value(chainId).netVersion().flowable()

    private fun getTransaction(
        from: String,
        count: EthGetTransactionCount,
        to: String,
        amount: BigDecimal,
        contractData: String
    ) =
        Transaction(
            from,
            count.transactionCount,
            BigInteger.ZERO,
            BigInteger.ZERO,
            to,
            toWei(amount, Convert.Unit.ETHER).toBigInteger(),
            contractData
        )

    private fun estimateGasLimit(gasLimit: BigInteger): BigInteger =
        if (gasLimit == Operation.DEFAULT_LIMIT.gasLimit) {
            gasLimit
        } else {
            increaseGasLimitByTenPercent(gasLimit)
        }

    private fun increaseGasLimitByTenPercent(gasLimit: BigInteger) = gasLimit.add(getBuffer(gasLimit))

    private fun getBuffer(gasLimit: BigInteger) =
        gasLimit.multiply(BigInteger.valueOf(10)).divide(BigInteger.valueOf(100))

    private fun calculateTransactionCosts(
        chainId: Int,
        gasLimit: BigInteger,
        gasPrice: BigDecimal?
    ): Single<TransactionCostPayload> =
        Single.just(
            TransactionCostPayload(
                convertGasPrice(gasPrice, chainId),
                gasLimit,
                getTransactionCostInEth(getGasPrice(gasPrice, chainId), BigDecimal(gasLimit))
            )
        )

    private fun convertGasPrice(gasPrice: BigDecimal?, chainId: Int) =
        if (gasPrice == null) fromWei(BigDecimal(gasPrices.value(chainId)), Convert.Unit.GWEI)
        else fromWei(gasPrice, Convert.Unit.GWEI)

    private fun getGasPrice(gasPrice: BigDecimal?, chainId: Int) =
        gasPrice ?: BigDecimal(gasPrices.value(chainId))

    private fun getSignedEtherTransaction(
        count: BigInteger,
        transactionPayload: TransactionPayload,
        chainId: Long
    ): String? =
        Numeric.toHexString(
            TransactionEncoder.signMessage(
                createEtherTransaction(count, transactionPayload), chainId,
                Credentials.create(transactionPayload.privateKey)
            )
        )

    private fun getSignedTransaction(
        count: BigInteger,
        transactionPayload: TransactionPayload,
        chainId: Long,
        data: ByteArray
    ): String? =
        Numeric.toHexString(
            TransactionEncoder.signMessage(
                createTransaction(count, transactionPayload, data), chainId,
                Credentials.create(transactionPayload.privateKey)
            )
        )

    private fun createTransaction(count: BigInteger, payload: TransactionPayload, data: ByteArray): RawTransaction =
        RawTransaction.createTransaction(
            count,
            toWei(payload.gasPrice, Convert.Unit.GWEI).toBigInteger(),
            payload.gasLimit,
            payload.receiverAddress,
            toWei(payload.amount, Convert.Unit.ETHER).toBigInteger(),
            Numeric.toHexString(data)
        )

    private fun createEtherTransaction(count: BigInteger, payload: TransactionPayload): RawTransaction? =
        RawTransaction.createEtherTransaction(
            count,
            toWei(payload.gasPrice, Convert.Unit.GWEI).toBigInteger(),
            payload.gasLimit,
            payload.receiverAddress,
            toWei(payload.amount, Convert.Unit.ETHER).toBigInteger()
        )

    companion object {
        private const val START = 0
        private const val SCALE = 8
        private const val DOT = "."
        private const val BLOCK_NUMBER_OFFSET = 5L
        private const val TIMEOUT = 5L
        private const val CORRECT_ATS_FREE_PREFIX = "txHash"
    }
}