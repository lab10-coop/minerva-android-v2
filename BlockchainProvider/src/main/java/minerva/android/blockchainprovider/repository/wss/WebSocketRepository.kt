package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import java.math.BigInteger

interface WebSocketRepository {
    fun subscribeToExecutedTransactions(chainId: Int, blockNumber: BigInteger): Flowable<ExecutedTransaction>
    fun subscribeToBlockCreation(chainId: Int): Flowable<Unit>
    fun disconnect()
}