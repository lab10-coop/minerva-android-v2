package minerva.android.blockchainprovider.repository.regularAccont

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.PendingTransaction
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal
import java.math.BigInteger

interface BlockchainRegularAccountRepository {
    fun refreshBalances(networkAddress: List<Pair<String, String>>): Single<List<Pair<String, BigDecimal>>>
    fun refreshAssetBalance(
        privateKey: String,
        network: String,
        contractAddress: String,
        safeAccountAddress: String = String.Empty
    ): Observable<Pair<String, BigDecimal>>

    fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal): BigDecimal
    fun transferNativeCoin(network: String, accountIndex: Int, transactionPayload: TransactionPayload): Single<PendingTransaction>
    fun toGwei(amount: BigDecimal): BigDecimal
    fun transferERC20Token(network: String, payload: TransactionPayload): Completable
    fun reverseResolveENS(ensAddress: String): Single<String>
    fun resolveENS(ensName: String): Single<String>
    fun getTransactions(pendingHashes: List<Pair<String, String>>): Single<List<Pair<String, String?>>>
    fun getTransactionCosts(
        network: String,
        assetIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal,
        gasPrice: BigDecimal? = null
    ): Single<TransactionCostPayload>

    fun isAddressValid(address: String): Boolean
    fun getCurrentBlockNumber(network: String): Flowable<BigInteger>
    fun toChecksumAddress(address: String): String
    fun getFreeATS(address: String): Completable
}