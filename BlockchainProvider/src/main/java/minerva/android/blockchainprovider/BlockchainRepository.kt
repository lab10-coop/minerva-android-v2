package minerva.android.blockchainprovider

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.math.BigDecimal
import java.math.BigInteger

interface BlockchainRepository {

    fun refreshBalances(networkAddress: List<Pair<String, String>>): Single<List<Pair<String, BigDecimal>>>
    fun refreshAssetBalance(privateKey: String, network: String, contractAddress: String): Observable<Pair<String, BigDecimal>>
    fun transferERC20Token(privateKey: String, network: String, toAddress: String, contractAddress: String): Observable<TransactionReceipt>
    fun getTransactionCosts(network: String): Single<TransactionCostPayload>
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal
    fun sendTransaction(network: String, transactionPayload: TransactionPayload): Completable
    fun completeAddress(privateKey: String): String
    fun toGwei(balance: BigDecimal): BigInteger
}