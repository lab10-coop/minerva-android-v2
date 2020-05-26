package minerva.android.blockchainprovider.repository.blockchain

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.contract.ERC20
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.provider.ContractGasProvider
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import org.web3j.utils.Convert.fromWei
import org.web3j.utils.Convert.toWei
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode


class BlockchainRepositoryImpl(
    private val web3j: Map<String, Web3j>,
    private val gasPrice: Map<String, BigInteger>,
    private val ensResolver: EnsResolver
) : BlockchainRepository {

    /**
     * List arguments: first - network short name, second - wallet address (public)
     */
    override fun refreshBalances(networkAddress: List<Pair<String, String>>): Single<List<Pair<String, BigDecimal>>> =
        Observable.range(START, networkAddress.size)
            .flatMapSingle { position -> getBalance(networkAddress[position].first, networkAddress[position].second) }
            .toList()

    override fun refreshAssetBalance(
        privateKey: String,
        network: String,
        contractAddress: String,
        safeAccountAddress: String
    ): Observable<Pair<String, BigDecimal>> =
        if (safeAccountAddress.isEmpty()) getERC20Balance(contractAddress, network, privateKey, Credentials.create(privateKey).address)
        else getERC20Balance(contractAddress, network, privateKey, safeAccountAddress)

    private fun getERC20Balance(
        contractAddress: String,
        network: String,
        privateKey: String,
        address: String
    ): Observable<Pair<String, BigDecimal>> =
        ERC20.load(
            contractAddress, web3j[network], Credentials.create(privateKey),
            ContractGasProvider(gasPrice[network] ?: error("Not supported Network"), Operation.TRANSFER_ERC20.gasLimit)
        )
            .balanceOf(address).flowable()
            .map { balance -> Pair(contractAddress, fromWei(balance.toString(), Convert.Unit.ETHER)) }
            .toObservable()

    override fun reverseResolveENS(ensAddress: String): Single<String> {
        return Single.just(ensAddress).map { ensResolver.reverseResolve(it) }
    }

    override fun resolveENS(ensName: String): Single<String> =
        if (ensName.contains(DOT)) Single.just(ensName).map { ensResolver.resolve(it) }
        else Single.just(ensName)

    override fun transferERC20Token(network: String, payload: TransactionPayload): Completable {
        Credentials.create(payload.privateKey).run {
            return ERC20.load(payload.contractAddress, web3j[network], this, ContractGasProvider(toGwei(payload.gasPrice), payload.gasLimit))
                .transfer(payload.receiverKey, toWei(payload.amount, Convert.Unit.ETHER).toBigInteger()).flowable().toObservable()
                .ignoreElements()
        }
    }

    override fun transferNativeCoin(network: String, transactionPayload: TransactionPayload): Completable =
        (web3j[network] ?: error("Not supported Network!"))
            .ethGetTransactionCount(transactionPayload.address, DefaultBlockParameterName.LATEST)
            .flowable()
            .flatMapCompletable {
                (web3j[network] ?: error("Not supported Network!"))
                    .ethSendRawTransaction(getSignedTransaction(it.transactionCount, transactionPayload))
                    .flowable()
                    .flatMapCompletable { response -> handleTransactionResponse(response) }
            }

    override fun getTransactionCosts(network: String, assetIndex: Int, operation: Operation): TransactionCostPayload =
        prepareTransactionCosts((gasPrice[network] ?: error("Not supported Network!")), operation.gasLimit)

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        getTransactionCostInEth(toWei(gasPrice, Convert.Unit.GWEI), BigDecimal(gasLimit))

    override fun toGwei(balance: BigDecimal): BigInteger = toWei(balance, Convert.Unit.GWEI).toBigInteger()

    private fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal) =
        fromWei((gasPrice * gasLimit), Convert.Unit.ETHER).setScale(SCALE, RoundingMode.HALF_EVEN)

    private fun handleTransactionResponse(response: EthSendTransaction) =
        if (response.error == null) Completable.complete()
        else Completable.error(Throwable(response.error.message))

    private fun getSignedTransaction(count: BigInteger, transactionPayload: TransactionPayload): String? =
        Numeric.toHexString(
            TransactionEncoder.signMessage(
                createTransaction(count, transactionPayload),
                Credentials.create(transactionPayload.privateKey)
            )
        )

    private fun createTransaction(count: BigInteger, transactionPayload: TransactionPayload): RawTransaction? =
        transactionPayload.run {
            return RawTransaction.createEtherTransaction(
                count,
                toWei(gasPrice, Convert.Unit.GWEI).toBigInteger(),
                gasLimit,
                receiverKey,
                toWei(amount, Convert.Unit.ETHER).toBigInteger()
            )
        }

    private fun prepareTransactionCosts(gasPrice: BigInteger, gasLimit: BigInteger = Transfer.GAS_LIMIT): TransactionCostPayload =
        TransactionCostPayload(
            fromWei(BigDecimal(gasPrice), Convert.Unit.GWEI),
            gasLimit,
            getTransactionCostInEth(BigDecimal(gasPrice), BigDecimal(gasLimit))
        )

    private fun getBalance(network: String, address: String): Single<Pair<String, BigDecimal>> =
        (web3j[network] ?: error("Not supported Network! ($network)")).ethGetBalance(address, DefaultBlockParameterName.LATEST)
            .flowable()
            .map { Pair(address, fromWei(BigDecimal(it.balance), Convert.Unit.ETHER)) }
            .firstOrError()

    companion object {
        private const val START = 0
        private const val SCALE = 8
        private const val DOT = "."
    }
}