package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.kotlinUtils.map.value
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.websocket.WebSocketClient
import org.web3j.protocol.websocket.WebSocketService
import timber.log.Timber
import java.math.BigInteger
import java.net.URI
import java.util.concurrent.TimeUnit

class WebSocketServiceProviderImpl : WebSocketServiceProvider {

    override fun subscribeToExecutedTransactions(web3j: Web3j, blockNumber: BigInteger): Flowable<ExecutedTransaction> =
        web3j.replayPastAndFutureTransactionsFlowable(DefaultBlockParameter.valueOf(blockNumber))
            .take(TIMEOUT, TimeUnit.SECONDS)
            .map { ExecutedTransaction(it.hash, it.from) }
}

private const val TIMEOUT = 120L