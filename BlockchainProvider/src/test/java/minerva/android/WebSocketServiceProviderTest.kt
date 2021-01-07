package minerva.android

import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import minerva.android.blockchainprovider.repository.wss.WebSocketServiceProviderImpl
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.Transaction
import java.math.BigInteger

class WebSocketServiceProviderTest : RxTest() {

    private val web3J = mockk<Web3j>()
    private val provider = WebSocketServiceProviderImpl()


    @Test
    fun `subscribe to executed transactions success test`() {
        val transaction = Transaction()
        transaction.hash = "0x1234"
        transaction.from = "from"
        every { web3J.replayPastAndFutureTransactionsFlowable(any()) } returns Flowable.just(transaction)
        provider.subscribeToExecutedTransactions(web3J, BigInteger.ONE)
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.txHash == "0x1234" &&
                        it.senderAddress == "from"
            }
    }

    @Test
    fun `subscribe to executed transactions error test`() {
        val error = Throwable()
        every { web3J.replayPastAndFutureTransactionsFlowable(any()) } returns Flowable.error(error)
        provider.subscribeToExecutedTransactions(web3J, BigInteger.ONE)
            .test()
            .await()
            .assertError(error)
    }

}