package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import java.math.BigInteger
import java.net.ConnectException

interface WebSocketRepository {
    @Throws(ConnectException::class)
    fun subscribeToExecutedTransactions(chainId: Int, blockNumber: BigInteger): Flowable<ExecutedTransaction>
    @Throws(ConnectException::class)
    fun subscribeToBlockCreation(chainId: Int): Flowable<Unit>
    fun disconnect()
}