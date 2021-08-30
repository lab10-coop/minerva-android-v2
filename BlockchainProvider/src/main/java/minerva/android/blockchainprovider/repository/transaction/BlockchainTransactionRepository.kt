package minerva.android.blockchainprovider.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.blockchainprovider.model.*
import java.math.BigDecimal

interface BlockchainTransactionRepository {
    fun getCoinBalances(addresses: List<Pair<Int, String>>): Flowable<Token>
    fun getTransactionCostInEth(gasPrice: BigDecimal, gasLimit: BigDecimal): BigDecimal
    fun transferNativeCoin(chainId: Int, accountIndex: Int, transactionPayload: TransactionPayload): Single<PendingTransaction>
    fun sendWalletConnectTransaction(chainId: Int, transactionPayload: TransactionPayload): Single<String>
    fun getTransactions(pendingHashes: List<Pair<Int, String>>): Single<List<Pair<String, String?>>>
    fun getTransactionCosts(txCostData: TxCostData, gasPrice: BigDecimal? = null): Single<TransactionCostPayload>
    fun getFreeATS(address: String): Completable
}