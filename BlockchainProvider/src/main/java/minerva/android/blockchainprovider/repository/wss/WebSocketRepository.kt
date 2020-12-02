package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import org.web3j.protocol.core.methods.response.EthBlockNumber
import org.web3j.protocol.websocket.WebSocketService
import java.math.BigInteger

interface WebSocketRepository {
    fun subscribeToExecutedTransactions(network: String, blockNumber: BigInteger): Flowable<ExecutedTransaction>
}