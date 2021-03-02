package minerva.android.blockchainprovider.repository.regularAccont

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.PendingTransaction
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.model.TxCostData
import java.math.BigDecimal
import java.math.BigInteger

interface BlockchainRegularAccountRepository {
    fun refreshBalances(networkAddress: List<Pair<Int, String>>): Single<List<Pair<String, BigDecimal>>>

    fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal): BigDecimal
    fun transferNativeCoin(
        chainId: Int,
        accountIndex: Int,
        transactionPayload: TransactionPayload
    ): Single<PendingTransaction>

    fun toGwei(amount: BigDecimal): BigDecimal
    fun fromGwei(amount: BigDecimal): BigDecimal
    fun transferERC20Token(chainId: Int, payload: TransactionPayload): Completable
    fun reverseResolveENS(ensAddress: String): Single<String>
    fun resolveENS(ensName: String): Single<String>
    fun getTransactions(pendingHashes: List<Pair<Int, String>>): Single<List<Pair<String, String?>>>
    fun getTransactionCosts(
        chainId: Int,
        assetIndex: Int,
        from: String,
        to: String,
        amount: BigDecimal,
        gasPrice: BigDecimal? = null,
        contractData: String = String.Empty
    ): Single<TransactionCostPayload>

    fun getTransactionCosts(txCostData: TxCostData, gasPrice: BigDecimal? = null): Single<TransactionCostPayload>

    fun isAddressValid(address: String): Boolean
    fun getCurrentBlockNumber(chainId: Int): Flowable<BigInteger>
    fun toChecksumAddress(address: String): String
    fun getFreeATS(address: String): Completable
    fun getERC20TokenName(privateKey: String, chainId: Int, tokenAddress: String): Observable<String>
    fun getERC20TokenSymbol(privateKey: String, chainId: Int, tokenAddress: String): Observable<String>
    fun getERC20TokenDecimals(privateKey: String, chainId: Int, tokenAddress: String): Observable<BigInteger>
    fun fromWei(value: BigDecimal): BigDecimal
    fun toEther(value: BigDecimal): BigDecimal
    fun sendTransaction(chainId: Int, transactionPayload: TransactionPayload): Single<String>
    fun refreshTokenBalance(
        privateKey: String,
        chainId: Int,
        contractAddress: String,
        safeAccountAddress: String
    ): Observable<Pair<String, BigDecimal>>
}