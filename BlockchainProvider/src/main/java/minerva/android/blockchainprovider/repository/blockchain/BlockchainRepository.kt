package minerva.android.blockchainprovider.repository.blockchain

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal
import java.math.BigInteger

interface BlockchainRepository {
    fun refreshBalances(networkAddress: List<Pair<String, String>>): Single<List<Pair<String, BigDecimal>>>
    fun refreshAssetBalance(
        privateKey: String,
        network: String,
        contractAddress: String,
        safeAccountAddress: String = String.Empty
    ): Observable<Pair<String, BigDecimal>>
    fun getTransactionCosts(network: String, assetIndex: Int, operation: Operation): TransactionCostPayload
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal
    fun transferNativeCoin(network: String, transactionPayload: TransactionPayload): Completable
    fun toGwei(balance: BigDecimal): BigInteger
    fun transferERC20Token(network: String, transactionPayload: TransactionPayload): Completable
    fun reverseResolveENS(ensAddress: String): Single<String>
    fun resolveENS(ensName: String): Single<String>
}