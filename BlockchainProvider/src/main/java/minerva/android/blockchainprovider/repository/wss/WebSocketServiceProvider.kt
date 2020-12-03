package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.kotlinUtils.map.value
import org.web3j.protocol.Web3j
import org.web3j.protocol.websocket.WebSocketClient
import org.web3j.protocol.websocket.WebSocketService
import java.math.BigInteger
import java.net.URI

interface WebSocketServiceProvider {
    fun subscribeToExecutedTransactions(web3j: Web3j, blockNumber: BigInteger): Flowable<ExecutedTransaction>
}