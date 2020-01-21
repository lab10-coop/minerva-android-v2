package minerva.android.blockchainprovider

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

class BlockchainProvider(blockchainURL: String) {

    private val web3j = Web3j.build(HttpService(blockchainURL))

    fun refreshBalances(publicKeys: List<String>): Single<List<Pair<String, BigInteger>>> {
        return Observable.range(START, publicKeys.size)
            .flatMapSingle { position ->
                getBalance(publicKeys[position])
            }.toList()
    }

    fun getBalance(publicKey: String): Single<Pair<String, BigInteger>> {
        return web3j.ethGetBalance(publicKey, DefaultBlockParameterName.LATEST)
            .flowable()
            .map { Pair(publicKey, it.balance) }
            .firstOrError()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    companion object {
        private const val START = 0
    }
}