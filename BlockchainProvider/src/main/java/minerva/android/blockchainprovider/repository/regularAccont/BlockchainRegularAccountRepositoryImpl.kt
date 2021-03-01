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
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
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
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Pair

class BlockchainRegularAccountRepositoryImpl(
    private val web3j: Map<String, Web3j>,
    private val gasPrices: Map<String, BigInteger>,
    private val ensResolver: EnsResolver,
    private val freeTokenRepository: FreeTokenRepository
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
                        getSignedEtherTransaction(
                            it.first.transactionCount,
                            transactionPayload,
                            it.second.netVersion.toLong()
                        )
                    )
                    .flowable()
                    .zipWith(getCurrentBlockNumber(network))
                    .flatMapSingle { (response, blockNumber) ->
                        if (response.error == null) {
                            val pendingTx = PendingTransaction(
                                accountIndex,
                                response.transactionHash,
                                network,
                                transactionPayload.senderAddress,
                                String.Empty,
                                transactionPayload.amount,
                                blockNumber.subtract(BigInteger.valueOf(BLOCK_NUMBER_OFFSET)) //get n-5 block number, where n is current block number
                            )
                            Single.just(pendingTx)
                        } else Single.error(Throwable(response.error.message))
                    }
            }.firstOrError()

    override fun getCurrentBlockNumber(network: String): Flowable<BigInteger> =
        web3j.value(network)
            .ethBlockNumber()
            .flowable()
            .map { it.blockNumber }

    /**
     * List arguments: first - network short name, second - wallet address (public)
     */
    override fun refreshBalances(networkAddress: List<Pair<String, String>>): Single<List<Pair<String, BigDecimal>>> =
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

    override fun getERC20TokenName(privateKey: String, network: String, tokenAddress: String): Observable<String> =
        getChainId(network).flatMap {
            loadERC20(privateKey, network, tokenAddress, it.netVersion.toLong()).name().flowable()
        }.toObservable()

    override fun getERC20TokenSymbol(privateKey: String, network: String, tokenAddress: String): Observable<String> =
        getChainId(network).flatMap {
            loadERC20(privateKey, network, tokenAddress, it.netVersion.toLong()).symbol().flowable()
        }.toObservable()

    override fun getERC20TokenDecimals(
        privateKey: String,
        network: String,
        tokenAddress: String
    ): Observable<BigInteger> =
        getChainId(network).flatMap {
            loadERC20(privateKey, network, tokenAddress, it.netVersion.toLong()).decimals().flowable()
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

    private var txData = String.Empty

    //EOA (with contract byte code) -> TOKEN
    //use it for:
    //token to token
    //sth to token
    //token to sth
    override fun sendTransaction(network: String, transactionPayload: TransactionPayload): Single<String> =
        web3j.value(network)
            .ethGetTransactionCount(transactionPayload.senderAddress, DefaultBlockParameterName.LATEST)
            .flowable()
            .zipWith(getChainId(network))
            .flatMap {

                Timber.tag("kobe").d("GasLimit: ${transactionPayload.gasLimit}")

                Timber.tag("kobe").d("SEND 2 txData: $txData")


                web3j.value(network)
                    .ethSendRawTransaction(
                        getSignedTransaction(
                            it.first.transactionCount,
                            transactionPayload,
                            it.second.netVersion.toLong(),
//                            Numeric.hexStringToByteArray(txData)
                            Numeric.hexStringToByteArray(transactionPayload.data)
                        )
                    )
                    .flowable()
                    .flatMapSingle { response ->
                        if (response.error == null) {
                            Timber.tag("kobe").d("Transfer: ${response.transactionHash}")

                            Single.just(response.transactionHash)
                        } else {
                            Timber.tag("kobe").d("Transfer error message: ${response.error.message}")
                            Timber.tag("kobe").d("Transfer error data: ${response.error.data}")

                            Single.error(Throwable(response.error.message))
                        }
                    }
            }.firstOrError()

    //TOKEN -> TOKEN
    override fun transferERC20Token(network: String, payload: TransactionPayload): Completable =//Single<String> =
        getChainId(network).flatMapCompletable {

            Timber.tag("kobe").d("GasLimit transfer: ${payload.gasLimit}")
            Timber.tag("kobe").d("GasPrice transfer: ${payload.gasPrice}")


            loadTransactionERC20(payload.privateKey, network, payload.contractAddress, it.netVersion.toLong(), payload)

                .transfer(payload.receiverAddress, toWei(payload.amount, Convert.Unit.ETHER).toBigInteger())
                .flowable()
                .map {
                    Timber.tag("kobe").d("ERC20 Token transfer: ${it.transactionHash}")
                    it.transactionHash
                }.ignoreElements()

        }

    override fun getTransactionCosts(
        network: String,
        assetIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal,
        gasPrice: BigDecimal?,
        contractAddress: String,
        contractData: String
    ): Single<TransactionCostPayload> =
        if (assetIndex == Int.InvalidIndex) {
            Timber.tag("kobe").d("native coin: $assetIndex")
            //eth -> eth
            //eth -> token
            web3j.value(network).ethGetTransactionCount(from, DefaultBlockParameterName.LATEST)
                .flowable()
                .zipWith(resolveENS(to).toFlowable())
                .flatMap { (count, address) ->

                    web3j.value(network).ethEstimateGas(
                        getTransaction(
                            from,
                            count,
                            address,
                            amount,
                            contractData
                        )
                    )
                        .flowable()
                        .flatMapSingle { handleGasLimit(network, it, gasPrice) }
                }
                .firstOrError()
                .timeout(
                    TIMEOUT,
                    TimeUnit.SECONDS,
                    calculateTransactionCosts(network, Operation.TRANSFER_NATIVE.gasLimit, gasPrice)
                )

        } else {
            //token to token
            web3j.value(network).ethGetTransactionCount(from, DefaultBlockParameterName.LATEST)
                .flowable()
//                .zipWith(resolveENS(to).toFlowable())
                .flatMap { count ->

                    Timber.tag("kobe").d("Amount to send: $amount")
                    Timber.tag("kobe").d("Amount to send in BIGInteger: ${amount.toBigInteger()}")
                    Timber.tag("kobe").d("Amount to send in ETH: ${toWei(amount, Convert.Unit.ETHER).toBigInteger()}")
                    Timber.tag("kobe").d("To: $to")
                    Timber.tag("kobe").d("From: $from")
                    Timber.tag("kobe").d("ContractAddress: $contractAddress")

                    //address - receiver token address
                    //smart contract address

                    val function =
                        Function(
                            ERC20.FUNC_TRANSFER,
                            listOf(Address(to), Uint256(toWei(amount, Convert.Unit.ETHER).toBigInteger())),
                            emptyList()
                        )

                    txData = FunctionEncoder.encode(function)

                    //czy token address to smartContract address
                    //

                    Timber.tag("kobe").d("1 TxData: $txData")

                    val functionCallTransaction = Transaction.createFunctionCallTransaction(
                        from, //xDai address
                        count.transactionCount,
                        BigInteger.ZERO, //gasPrice
                        BigInteger.ZERO, //gasLimit
                        contractAddress,//HNY token contractAddress     //token smart contract address
                        BigInteger.ZERO, //value of native coin
                        txData // amount of HNY token and receiver address (xDai address )
                    )


                    web3j.value(network).ethEstimateGas(functionCallTransaction)
                        .flowable()
                        .zipWith(
                            web3j.value(network).ethCall(functionCallTransaction, DefaultBlockParameterName.LATEST)
                                .flowable()
                        )
                        .flatMapSingle { (gas, eth) ->

                            Timber.tag("kobe").d("Revert reason: ${eth.revertReason}")
                            if (eth.error != null) {
                                Timber.tag("kobe").d("Error data: ${eth.error.data}")
                                Timber.tag("kobe").d("Error message: ${eth.error.message}")
                            }
                            Timber.tag("kobe").d("Raw response: ${eth.rawResponse}")

                            handleGasLimit(network, gas, gasPrice)
                        }
                }
                .firstOrError()
                .timeout(
                    TIMEOUT,
                    TimeUnit.SECONDS,
                    calculateTransactionCosts(network, Operation.TRANSFER_ERC20.gasLimit, gasPrice)
                )
        }

    private fun getTransaction(
        from: String,
        count: EthGetTransactionCount,
        to: String,
        amount: BigDecimal,
        contractData: String
    ) = Transaction(
        from,
        count.transactionCount,
        BigInteger.ZERO,
        BigInteger.ZERO,
        to,
        toWei(amount, Convert.Unit.ETHER).toBigInteger(),
        contractData
    )

    private fun handleGasLimit(
        network: String,
        it: EthEstimateGas,
        gasPrice: BigDecimal?
    ): Single<TransactionCostPayload> {
        val gasLimit = it.error?.let {
            Timber.tag("kobe").d("estimate gas error: ${it.message}, data: ${it.data}")
            Operation.TRANSFER_NATIVE.gasLimit
        } ?: it.amountUsed//estimateGasLimit(it.amountUsed)

        Timber.tag("kobe").d("SEND gasLimit: $gasLimit")
        Timber.tag("kobe").d("calculated gasLimit: ${increaseGasLimitByTenPercent(gasLimit)}")


        return calculateTransactionCosts(network, increaseGasLimitByTenPercent(gasLimit), gasPrice)
    }

    private fun estimateGasLimit(gasLimit: BigInteger): BigInteger =
        if (gasLimit == Operation.DEFAULT_LIMIT.gasLimit) {
            Timber.tag("kobe").d("default gasLimit: $gasLimit")
            gasLimit
        } else {
            Timber.tag("kobe").d("calculated gasLimit: ${increaseGasLimitByTenPercent(gasLimit)}")
            increaseGasLimitByTenPercent(gasLimit)
        }

    override fun refreshTokenBalance(
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


    override fun toGwei(amount: BigDecimal): BigDecimal = toWei(amount, Convert.Unit.GWEI)
    override fun fromWei(value: BigDecimal): BigDecimal = fromWei(value, Convert.Unit.GWEI)
    override fun toEther(value: BigDecimal): BigDecimal = fromWei(value, Convert.Unit.ETHER)

    override fun fromGwei(amount: BigDecimal): BigDecimal = fromWei(amount, Convert.Unit.ETHER)

    override fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal): BigDecimal =
        toEther((gasPrice * gasLimit)).setScale(SCALE, RoundingMode.HALF_EVEN)

    private fun loadERC20(privateKey: String, network: String, address: String, chainId: Long) =
        ERC20.load(
            address, web3j.value(network),
            RawTransactionManager(
                web3j.value(network),
                Credentials.create(privateKey),
                chainId
            ),
            ContractGasProvider(gasPrices.value(network), Operation.TRANSFER_ERC20.gasLimit)
        )

    //todo will be removed within the MNR-403
    private fun loadTransactionERC20(
        privateKey: String,
        network: String,
        address: String,
        chainId: Long,
        payload: TransactionPayload
    ) = ERC20.load(
        address, web3j.value(network),
        RawTransactionManager(
            web3j.value(network),
            Credentials.create(privateKey),
            chainId
        ),
        ContractGasProvider(payload.gasPriceWei, payload.gasLimit)
    )

    private fun getBalance(network: String, address: String): Single<Pair<String, BigDecimal>> =
        web3j.value(network).ethGetBalance(address, DefaultBlockParameterName.LATEST)
            .flowable()
            .map { Pair(address, toEther(BigDecimal(it.balance))) }
            .firstOrError()

    private fun getERC20Balance(
        contractAddress: String,
        network: String,
        privateKey: String,
        address: String
    ): Observable<Pair<String, BigDecimal>> =
        getChainId(network)
            .flatMap {
                loadERC20(privateKey, network, contractAddress, it.netVersion.toLong()).balanceOf(address).flowable()
                    .map { balance -> Pair(contractAddress, balance.toBigDecimal()) }
            }.toObservable()

    private fun getChainId(network: String): Flowable<NetVersion> = web3j.value(network).netVersion().flowable()

    private fun increaseGasLimitByTenPercent(gasLimit: BigInteger) = gasLimit.add(getBuffer(gasLimit))

    private fun getBuffer(gasLimit: BigInteger) =
        gasLimit.multiply(BigInteger.valueOf(10)).divide(BigInteger.valueOf(100))

    private fun calculateTransactionCosts(
        network: String,
        gasLimit: BigInteger,
        gasPrice: BigDecimal?
    ): Single<TransactionCostPayload> =
        Single.just(
            TransactionCostPayload(
                convertGasPrice(gasPrice, network),
                gasLimit,
                getTransactionCostInEth(getGasPrice(gasPrice, network), BigDecimal(gasLimit))
            )
        )

    private fun convertGasPrice(gasPrice: BigDecimal?, network: String) =
        if (gasPrice == null) fromWei(BigDecimal(gasPrices.value(network)), Convert.Unit.GWEI)
        else fromWei(gasPrice, Convert.Unit.GWEI)

    private fun getGasPrice(gasPrice: BigDecimal?, network: String) =
        gasPrice ?: BigDecimal(gasPrices.value(network))

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