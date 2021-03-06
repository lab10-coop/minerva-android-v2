package minerva.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepositoryImpl
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.kotlinUtils.InvalidIndex
import org.amshove.kluent.shouldEqual
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthGasPrice
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.BigInteger


class BlockchainRepositoryImplTest {

    private val ATS_GAS_PRICE = BigInteger.valueOf(100_000_000_000)
    private val ETH_GAS_PRICE = BigInteger.valueOf(20_000_000_000)

    private val ETH = "ETH"
    private val ATS = "ATS"
    private val web3J = mockk<Web3j>()
    private val web3Js: Map<String, Web3j> = mapOf(Pair(ETH, web3J))
    private val gasPrice: Map<String, BigInteger> = mapOf(Pair(ETH, ETH_GAS_PRICE), Pair(ATS, ATS_GAS_PRICE))
    private val blockchainRepository: BlockchainRepositoryImpl =
        BlockchainRepositoryImpl(web3Js, gasPrice)

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
        val ethCostPayload = blockchainRepository.getTransactionCosts(ETH, Int.InvalidIndex, Operation.TRANSFER_NATIVE)
        val atsCostPayload = blockchainRepository.getTransactionCosts(ATS, Int.InvalidIndex, Operation.TRANSFER_ERC20)
        val atsCostPayload2 = blockchainRepository.getTransactionCosts(ATS, Int.InvalidIndex, Operation.SAFE_ACCOUNT_TXS)
        ethCostPayload.gasPrice shouldEqual BigDecimal.valueOf(20)
        ethCostPayload.gasLimit shouldEqual BigInteger.valueOf(50000)
        atsCostPayload.gasPrice shouldEqual BigDecimal.valueOf(100)
        atsCostPayload.gasLimit shouldEqual BigInteger.valueOf(200000)
        atsCostPayload2.gasPrice shouldEqual BigDecimal.valueOf(100)
        atsCostPayload2.gasLimit shouldEqual BigInteger.valueOf(350000)
    }

    @Test
    fun `send transaction success test`() {
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val sendTransaction = EthSendTransaction()
        sendTransaction.result = "0x2"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.just(sendTransaction)
        blockchainRepository.transferNativeCoin(ETH, TransactionPayload("address", "0x2313"))
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
        blockchainRepository.transferNativeCoin(ETH, TransactionPayload("address", "0x2313"))
            .test()
            .assertError(error)
    }
}
