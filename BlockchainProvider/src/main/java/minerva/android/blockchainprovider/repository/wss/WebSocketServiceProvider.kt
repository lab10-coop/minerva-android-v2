package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import org.web3j.protocol.Web3j
import java.math.BigInteger

interface WebSocketServiceProvider {
    fun subscribeToExecutedTransactions(web3j: Web3j, blockNumber: BigInteger): Flowable<ExecutedTransaction>
}