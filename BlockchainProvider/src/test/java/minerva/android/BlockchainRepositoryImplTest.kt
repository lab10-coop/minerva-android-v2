package minerva.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.BlockchainRepositoryImpl
import minerva.android.blockchainprovider.model.TransactionPayload
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthGasPrice
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import java.math.BigInteger


class BlockchainRepositoryImplTest {

    private val ETH = "ETH"
    private val web3J = mockk<Web3j>()
    private val web3Js: Map<String, Web3j> = mapOf(Pair(ETH, web3J))
    private val blockchainRepository: BlockchainRepositoryImpl = BlockchainRepositoryImpl(web3Js)

    @get:Rule

    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `refresh balance success`() {
        val ethBalance = EthGetBalance()
        ethBalance.result = "0x1"
        every { web3J.ethGetBalance(any(), any()).flowable() } returns Flowable.just(ethBalance)
        blockchainRepository.refreshBalances(listOf(Pair(ETH, "0x9866208bea68b10f04697c00b891541a305df851")))
            .test()
            .await()
            .assertValue {
                it[0].first == "0x9866208bea68b10f04697c00b891541a305df851"
            }
    }

    @Test
    fun `refresh balance error`() {
        val error = Throwable()
        val ethBalance = EthGetBalance()
        ethBalance.result = "0x1"
        every { web3J.ethGetBalance(any(), any()).flowable() } returns Flowable.error(error)
        blockchainRepository.refreshBalances(listOf(Pair(ETH, "0x9866208bea68b10f04697c00b891541a305df851")))
            .test()
            .await()
            .assertError(error)
    }

    @Test
    fun `get transaction costs success test`() {
        val gasPrice = EthGasPrice()
        gasPrice.result = "0x1"
        every { web3J.ethGasPrice().flowable() } returns Flowable.just(gasPrice)
        blockchainRepository.getTransactionCosts(ETH)
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it.gasLimit == BigInteger.valueOf(21000)
            }
    }

    @Test
    fun `get transaction costs error test`() {
        val error = Throwable()
        val gasPrice = EthGasPrice()
        gasPrice.result = "0x1"
        every { web3J.ethGasPrice().flowable() } returns Flowable.error(error)
        blockchainRepository.getTransactionCosts(ETH)
            .test()
            .await()
            .assertError(error)
    }

    @Test
    fun `send transaction success test`() {
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val sendTransaction = EthSendTransaction()
        sendTransaction.result = "0x2"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.just(sendTransaction)
        blockchainRepository.sendTransaction(ETH, TransactionPayload("address", "0x2313"))
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction error test`() {
        val error = Throwable()
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val sendTransaction = EthSendTransaction()
        sendTransaction.result = "0x2"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.error(error)
        blockchainRepository.sendTransaction(ETH, TransactionPayload("address", "0x2313"))
            .test()
            .assertError(error)
    }
}
