package minerva.android.blockchainprovider.repository.wss

import io.reactivex.Flowable
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.model.PendingTransaction
import org.web3j.protocol.core.methods.response.Transaction

interface WebSocketServiceProvider {
    fun openConnection(network: String)
    fun subscribeToExecutedTransactions(network: String): Flowable<ExecutedTransaction>
}