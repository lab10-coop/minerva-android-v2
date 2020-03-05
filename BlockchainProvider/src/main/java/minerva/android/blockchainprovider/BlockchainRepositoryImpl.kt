package minerva.android.blockchainprovider

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.model.defs.BlockchainDef.Companion.ENS
import minerva.android.kotlinUtils.InvalidIndex
import org.web3j.contracts.eip20.generated.ERC20
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


class BlockchainRepositoryImpl(private val web3j: Map<String, Web3j>) : BlockchainRepository {

    /**
     * List arguments: first - network short name, second - wallet address (public)
     */
    override fun refreshBalances(networkAddress: List<Pair<String, String>>): Single<List<Pair<String, BigDecimal>>> =
        Observable.range(START, networkAddress.size)
            .flatMapSingle { position -> getBalance(networkAddress[position].first, networkAddress[position].second) }
            .toList()

    override fun refreshAssetBalance(privateKey: String, network: String, contractAddress: String): Observable<Pair<String, BigDecimal>> {
        Credentials.create(privateKey).run {
            return ERC20.load(contractAddress, web3j[network], this, DefaultContractGasProvider())
                .balanceOf(this.address).flowable()
                .map { balance -> Pair(contractAddress, fromWei(balance.toString(), Convert.Unit.ETHER)) }
                .toObservable()
        }
    }

    override fun reverseResolveENS(address: String): Single<String> = Single.just(address).map {
        EnsResolver(web3j[ENS]).reverseResolve(it)
    }

    override fun resolveENS(ensName: String): Single<String> =
        if (ensName.contains(DOT)) Single.just(ensName).map { EnsResolver(web3j[ENS]).resolve(it) }
        else Single.just(ensName)

    override fun transferERC20Token(
        network: String,
        payload: TransactionPayload
    ): Completable {
        Credentials.create(payload.privateKey).run {
            return ERC20.load(
                payload.contractAddress,
                web3j[network],
                this,
                AssetContractGasProvider(toGwei(payload.gasPrice), payload.gasLimit)
            )
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

    //TODO DefaultContractGasProvider is MVP hack. Needs to be refactored
    override fun getTransactionCosts(network: String, assetIndex: Int): Single<TransactionCostPayload> =
        if (assetIndex == Int.InvalidIndex) (web3j[network] ?: error("Not supported Network!"))
            .ethGasPrice()
            .flowable()
            .map { prepareTransactionCosts(it.gasPrice) }
            .singleOrError()
        else Single.just(DefaultContractGasProvider()).map { prepareTransactionCosts(it.gasPrice, it.gasLimit) }

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        getTransactionCostInEth(toWei(gasPrice, Convert.Unit.GWEI), BigDecimal(gasLimit))

    override fun completeAddress(privateKey: String): String = Credentials.create(privateKey).address

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