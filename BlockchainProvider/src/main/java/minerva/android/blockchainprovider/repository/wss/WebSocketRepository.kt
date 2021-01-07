package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import java.math.BigInteger

interface WebSocketRepository {
    fun subscribeToExecutedTransactions(network: String, blockNumber: BigInteger): Flowable<ExecutedTransaction>
}