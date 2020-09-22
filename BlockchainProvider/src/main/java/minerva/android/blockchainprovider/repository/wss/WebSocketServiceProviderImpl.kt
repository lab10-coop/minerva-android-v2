package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.kotlinUtils.map.value
import org.web3j.protocol.Web3j
import org.web3j.protocol.websocket.WebSocketClient
import org.web3j.protocol.websocket.WebSocketService
import java.net.URI
import java.util.concurrent.TimeUnit

class WebSocketServiceProviderImpl(private val wssUrls: Map<String, String>) : WebSocketServiceProvider {

    private lateinit var wssService: WebSocketService
    private lateinit var wssClient: WebSocketClient
    private var wssMap = mutableMapOf<String, WebSocketService>()

    override fun openConnection(network: String) {
        wssClient = WebSocketClient(URI(wssUrls.value(network)))
        wssService = WebSocketService(wssClient, false)
        wssMap.apply {
            this[network] = wssService
            getValue(network).connect()
        }
    }

    override fun subscribeToExecutedTransactions(network: String): Flowable<ExecutedTransaction> =
        Web3j.build(wssMap.getValue(network))
            .transactionFlowable()
            .take(TIMEOUT, TimeUnit.SECONDS)
            .map { ExecutedTransaction(it.hash, it.from) }
}

private const val TIMEOUT = 180L