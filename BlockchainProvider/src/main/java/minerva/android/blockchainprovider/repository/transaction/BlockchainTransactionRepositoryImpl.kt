package minerva.android.blockchainprovider.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.defs.BlockchainTransactionType
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.*
import minerva.android.blockchainprovider.repository.ens.ENSRepository
import minerva.android.blockchainprovider.repository.freeToken.FreeTokenRepository
import minerva.android.blockchainprovider.repository.units.UnitConverter
import minerva.android.blockchainprovider.smartContracts.ERC20
import minerva.android.blockchainprovider.utils.CryptoUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.map.value
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class BlockchainTransactionRepositoryImpl(
    private val web3j: Map<Int, Web3j>,
    private val gasPrices: Map<Int, BigInteger>,
    private val unitConverter: UnitConverter,
    private val ensRepository: ENSRepository,
    private val freeTokenRepository: FreeTokenRepository
) : BlockchainTransactionRepository {

    override fun getTransactions(pendingHashes: List<Pair<Int, String>>): Single<List<Pair<String, String?>>> =
        Observable.fromIterable(pendingHashes)
            .flatMapSingle { (chainId, txHash) -> getTransaction(chainId, txHash) }
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
            .flatMap {
                web3j.value(chainId)
                    .ethSendRawTransaction(
                        getSignedEtherTransaction(
                            it.transactionCount,
                            transactionPayload,
                            chainId.toLong()
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

    private fun createEtherTransaction(count: BigInteger, payload: TransactionPayload): RawTransaction? =
        RawTransaction.createEtherTransaction(
            count,
            Convert.toWei(payload.gasPrice, Convert.Unit.GWEI).toBigInteger(),
            payload.gasLimit,
            payload.receiverAddress,
            Convert.toWei(payload.amount, Convert.Unit.ETHER).toBigInteger()
        )

    private fun getCurrentBlockNumber(chainId: Int): Flowable<BigInteger> =
        web3j.value(chainId)
            .ethBlockNumber()
            .flowable()
            .map { ethBlock -> ethBlock.blockNumber }

    /**
     * List arguments: first - chainId, second - network short name, third - wallet address (public)
     */
    override fun getCoinBalances(addresses: List<Pair<Int, String>>): Flowable<Token> {
        val coinBalanceFlowables = mutableListOf<Flowable<Token>>()
        addresses.onEach { (chainId, address) ->
            coinBalanceFlowables.add(getCoinBalance(chainId, address).subscribeOn(Schedulers.io()))
        }
        return Flowable.mergeDelayError(coinBalanceFlowables)
    }

    private fun getCoinBalance(chainId: Int, address: String): Flowable<Token> =
        web3j.value(chainId).ethGetBalance(address, DefaultBlockParameterName.LATEST)
            .flowable()
            .map { ethBalance ->
                if (ethBalance.error != null) {
                    TokenWithError(chainId, address, Throwable(ethBalance.error.message))
                } else {
                    TokenWithBalance(chainId, address, unitConverter.toEther(BigDecimal(ethBalance.balance)))
                }
            }.onErrorReturn { error ->
                TokenWithError(chainId, address, error)
            }

    override fun getFreeATS(address: String): Completable = Completable.create {
        freeTokenRepository.getFreeATS(address).let { responseText ->
            if (responseText.startsWith(CORRECT_ATS_FREE_PREFIX)) it.onComplete()
            else it.onError(Throwable(responseText))
        }
    }

    override fun sendWalletConnectTransaction(chainId: Int, transactionPayload: TransactionPayload): Single<String> =
        web3j.value(chainId)
            .ethGetTransactionCount(transactionPayload.senderAddress, DefaultBlockParameterName.LATEST)
            .flowable()
            .flatMap { count ->
                web3j.value(chainId)
                    .ethSendRawTransaction(
                        getSignedTransaction(
                            count.transactionCount,
                            transactionPayload,
                            chainId.toLong()
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

    private fun getSignedTransaction(count: BigInteger, transactionPayload: TransactionPayload, chainId: Long): String? =
        Numeric.toHexString(
            TransactionEncoder.signMessage(
                createTransaction(count, transactionPayload), chainId,
                Credentials.create(transactionPayload.privateKey)
            )
        )

    private fun createTransaction(count: BigInteger, payload: TransactionPayload): RawTransaction =
        RawTransaction.createTransaction(
            count,
            Convert.toWei(payload.gasPrice, Convert.Unit.GWEI).toBigInteger(),
            payload.gasLimit,
            payload.receiverAddress,
            payload.amount.toBigInteger(),
            payload.data
        )

    override fun getTransactionCosts(txCostData: TxCostData, gasPrice: BigDecimal?): Single<TransactionCostPayload> =
        with(txCostData) {
            if (!isSafeAccountTransaction(txCostData)) {
                web3j.value(chainId).ethGetTransactionCount(from, DefaultBlockParameterName.LATEST)
                    .flowable()
                    .zipWith(ensRepository.resolveENS(to).toFlowable())
                    .flatMap { (count, address) ->
                        val transaction: Transaction = prepareTransaction(count, address, this)
                        web3j.value(chainId).ethEstimateGas(transaction)
                            .flowable()
                            .zipWith(
                                web3j.value(chainId)
                                    .ethCall(transaction, DefaultBlockParameterName.LATEST)
                                    .flowable()
                            )
                            .flatMapSingle { (gas, _) -> handleGasLimit(chainId, gas, gasPrice, txCostData.transferType) }
                    }
                    .firstOrError()
                    .timeout(
                        TIMEOUT,
                        TimeUnit.SECONDS,
                        calculateTransactionCosts(chainId, txCostData.transferType, gasPrice)
                    )
            } else {
                //TODO implement getting gasLimit for Safe Account transaction from Blockchain
                calculateTransactionCosts(chainId, Operation.SAFE_ACCOUNT_TXS.gasLimit, gasPrice)
            }
        }

    private fun calculateTransactionCosts(
        chainId: Int,
        transferType: BlockchainTransactionType,
        gasPrice: BigDecimal?
    ): Single<TransactionCostPayload> = Single.just(
        getGasLimitForTransferType(transferType).let { gasLimit ->
            TransactionCostPayload(
                convertGasPrice(gasPrice, chainId),
                gasLimit,
                getTransactionCostInEth(getGasPrice(gasPrice, chainId), BigDecimal(gasLimit))
            )
        }
    )

    private fun getGasLimitForTransferType(transferType: BlockchainTransactionType) =
        when(transferType){
            BlockchainTransactionType.COIN_TRANSFER, BlockchainTransactionType.COIN_SWAP -> Operation.TRANSFER_NATIVE.gasLimit
            BlockchainTransactionType.ERC721_TRANSFER -> Operation.TRANSFER_ERC721.gasLimit
            BlockchainTransactionType.ERC1155_TRANSFER -> Operation.TRANSFER_ERC1155.gasLimit
            else -> Operation.TRANSFER_ERC20.gasLimit
        }

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
        if (gasPrice == null) Convert.fromWei(BigDecimal(gasPrices.value(chainId)), Convert.Unit.GWEI)
        else Convert.fromWei(gasPrice, Convert.Unit.GWEI)

    private fun getGasPrice(gasPrice: BigDecimal?, chainId: Int) =
        gasPrice ?: BigDecimal(gasPrices.value(chainId))

    private fun prepareTransaction(count: EthGetTransactionCount, address: String, costData: TxCostData) =
        when (costData.transferType) {
            BlockchainTransactionType.TOKEN_TRANSFER,
            BlockchainTransactionType.ERC721_TRANSFER,
            BlockchainTransactionType.ERC1155_TRANSFER -> {
                Transaction.createFunctionCallTransaction(
                    costData.from,
                    count.transactionCount,
                    BigInteger.ZERO,
                    null,
                    costData.contractAddress,//token smart contract address
                    BigInteger.ZERO, //value of native coin
                    FunctionEncoder.encode( //data hex field, for token transactions
                        getTokenFunctionCall(
                            address,
                            CryptoUtils.convertTokenAmount(costData.amount, costData.tokenDecimals)
                        )
                    )
                )
            }
            else -> {
                Transaction(
                    costData.from,
                    count.transactionCount,
                    BigInteger.ZERO,
                    null,
                    costData.to,
                    Convert.toWei(costData.amount, Convert.Unit.ETHER).toBigInteger(),
                    costData.contractData
                )
            }
        }

    private fun getTokenFunctionCall(address: String, value: BigInteger) =
        Function(
            ERC20.FUNC_TRANSFER,
            listOf(Address(address), Uint256(value)),
            emptyList()
        )

    private fun isSafeAccountTransaction(txCostData: TxCostData) =
        txCostData.transferType == BlockchainTransactionType.SAFE_ACCOUNT_TOKEN_TRANSFER ||
                txCostData.transferType == BlockchainTransactionType.SAFE_ACCOUNT_COIN_TRANSFER

    private fun handleGasLimit(
        chainId: Int,
        it: EthEstimateGas,
        gasPrice: BigDecimal?,
        transferType: BlockchainTransactionType
    ): Single<TransactionCostPayload> {
        val gasLimit = it.error?.let {
                getGasLimitForTransferType(transferType)
        } ?: estimateGasLimit(it.amountUsed)
        return calculateTransactionCosts(chainId, gasLimit, gasPrice)
    }

    private fun estimateGasLimit(gasLimit: BigInteger): BigInteger =
        when (gasLimit) {
            Operation.DEFAULT_LIMIT.gasLimit -> gasLimit
            else -> increaseGasLimitByTenPercent(gasLimit)
        }

    private fun increaseGasLimitByTenPercent(gasLimit: BigInteger) = gasLimit.add(getBuffer(gasLimit))

    private fun getBuffer(gasLimit: BigInteger) =
        gasLimit.multiply(BigInteger.valueOf(10)).divide(BigInteger.valueOf(100))

    override fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal): BigDecimal =
        unitConverter.toEther((gasPrice * gasLimit)).setScale(SCALE, RoundingMode.HALF_EVEN)

    companion object {
        private const val TIMEOUT = 5L
        private const val CORRECT_ATS_FREE_PREFIX = "txHash"
        private const val BLOCK_NUMBER_OFFSET = 5L
        private const val SCALE = 8
    }
}