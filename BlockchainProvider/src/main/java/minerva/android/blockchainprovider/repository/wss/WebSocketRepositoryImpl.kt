package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.kotlinUtils.map.value
import org.web3j.protocol.Web3j
import org.web3j.protocol.websocket.WebSocketClient
import org.web3j.protocol.websocket.WebSocketService
import timber.log.Timber
import java.math.BigInteger
import java.net.URI

class WebSocketRepositoryImpl(
    private val provider: WebSocketServiceProvider,
    private val wssUrls: Map<String, String>
) : WebSocketRepository {

    private var wssClient: WebSocketClient? = null
    private lateinit var wssService: WebSocketService
    lateinit var web3j: Web3j

    override fun subscribeToExecutedTransactions(network: String, blockNumber: BigInteger): Flowable<ExecutedTransaction> {
        Timber.tag("kobe").d("Opening new connection: $network")
        openConnection(network)
        return provider.subscribeToExecutedTransactions(web3j, blockNumber)
    }

    private fun openConnection(network: String) {
        wssClient = WebSocketClient(URI(wssUrls.value(network)))
        wssService = WebSocketService(wssClient, false)
        wssService.connect()
        web3j = Web3j.build(wssService)
    }
}