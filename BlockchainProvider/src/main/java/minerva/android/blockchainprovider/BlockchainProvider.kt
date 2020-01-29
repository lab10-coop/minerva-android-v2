package minerva.android.blockchainprovider

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import org.web3j.utils.Convert.fromWei
import org.web3j.utils.Convert.toWei
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode


class BlockchainProvider(blockchainURL: String) {

    private val web3j = Web3j.build(HttpService(blockchainURL))

    fun refreshBalances(addresses: List<String>): Single<List<Pair<String, BigDecimal>>> {
        return Observable.range(START, addresses.size)
            .flatMapSingle { position ->
                getBalance(addresses[position])
            }.toList()
    }

    private fun getBalance(address: String): Single<Pair<String, BigDecimal>> {
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            .flowable()
            .map { Pair(address, fromWei(BigDecimal(it.balance), Convert.Unit.ETHER)) }
            .firstOrError()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun getTransactionCosts(): Single<Triple<BigDecimal, BigInteger, BigDecimal>> {
        return web3j.ethGasPrice().flowable()
            .map {
                Triple(
                    fromWei(BigDecimal(it.gasPrice), Convert.Unit.GWEI),
                    Transfer.GAS_LIMIT,
                    getTransactionCostInEth(BigDecimal(it.gasPrice), BigDecimal(Transfer.GAS_LIMIT))
                )
            }
            .singleOrError()
    }

    private fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal) =
        fromWei((gasPrice * gasLimit), Convert.Unit.ETHER).setScale(SCALE, RoundingMode.HALF_EVEN)

    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal {
        return getTransactionCostInEth(toWei(gasPrice, Convert.Unit.GWEI), BigDecimal(gasLimit))
    }

    fun sendTransaction(
        address: String,
        privateKey: String,
        receiverKey: String,
        amount: BigDecimal,
        gasPrice: BigDecimal,
        gasLimit: BigInteger
    ): Completable {
        return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
            .flowable()
            .flatMapCompletable {
                val signedTransaction = getSignedTransaction(it.transactionCount, gasPrice, gasLimit, receiverKey, amount, privateKey)
                web3j.ethSendRawTransaction(Numeric.toHexString(signedTransaction))
                    .flowable()
                    .flatMapCompletable { response ->
                        if (response.error == null) Completable.complete()
                        else Completable.error(Throwable(response.error.message))
                    }
            }
    }

    fun completeAddress(privateKey: String): String = Credentials.create(privateKey).address

    private fun getSignedTransaction(
        transactionCount: BigInteger,
        gasPrice: BigDecimal,
        gasLimit: BigInteger,
        receiverKey: String,
        amount: BigDecimal,
        privateKey: String
    ): ByteArray? {
        return TransactionEncoder.signMessage(
            createTransaction(Triple(transactionCount, gasPrice, gasLimit), receiverKey, amount),
            Credentials.create(privateKey)
        )
    }

    private fun createTransaction(
        transactionCosts: Triple<BigInteger, BigDecimal, BigInteger>,
        receiverKey: String,
        amount: BigDecimal
    ): RawTransaction? {
        return RawTransaction.createEtherTransaction(
            transactionCosts.first,
            toWei(transactionCosts.second, Convert.Unit.GWEI).toBigInteger(),
            transactionCosts.third,
            receiverKey,
            toWei(amount, Convert.Unit.ETHER).toBigInteger()
        )
    }

    companion object {
        private const val START = 0
        private const val SCALE = 8
    }
}