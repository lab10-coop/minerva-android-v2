package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.kotlinUtils.map.value
import org.web3j.protocol.Web3j
import org.web3j.protocol.websocket.WebSocketClient
import org.web3j.protocol.websocket.WebSocketService
import java.math.BigInteger
import java.net.ConnectException
import java.net.URI

class WebSocketRepositoryImpl(
    private val provider: WebSocketServiceProvider,
    private val wssUrls: Map<Int, String>
) : WebSocketRepository {

    private var wssClient: WebSocketClient? = null
    private var wssService: WebSocketService? = null
    private var web3j: Web3j? = null

    @Throws(ConnectException::class)
    override fun subscribeToExecutedTransactions(chainId: Int, blockNumber: BigInteger): Flowable<ExecutedTransaction> {
        openConnection(chainId)
        return provider.subscribeToExecutedTransactions(web3j!!, blockNumber)
    }

    @Throws(ConnectException::class)
    override fun subscribeToBlockCreation(chainId: Int): Flowable<Unit> {
        openConnection(chainId)
        return provider.subscribeToBlockCreation(web3j!!)
    }

    @Throws(ConnectException::class)
    private fun openConnection(chainId: Int) {
        wssClient = WebSocketClient(URI(wssUrls.value(chainId)))
        wssService = WebSocketService(wssClient, false)
        wssService?.connect()
        web3j = Web3j.build(wssService)
    }

    override fun disconnect() {
        wssService?.close()
        wssService = null
        wssClient = null
        web3j?.shutdown()
    }
}