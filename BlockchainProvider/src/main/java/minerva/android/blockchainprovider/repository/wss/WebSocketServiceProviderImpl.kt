package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class WebSocketServiceProviderImpl : WebSocketServiceProvider {

    override fun subscribeToExecutedTransactions(web3j: Web3j, blockNumber: BigInteger): Flowable<ExecutedTransaction> =
        web3j.replayPastAndFutureTransactionsFlowable(DefaultBlockParameter.valueOf(blockNumber))
            .take(TIMEOUT, TimeUnit.SECONDS)
            .map { ExecutedTransaction(it.hash, it.from) }
}

private const val TIMEOUT = 120L