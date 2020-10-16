package minerva.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepositoryImpl
import minerva.android.kotlinUtils.InvalidIndex
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals


class BlockchainRegularAccountRepositoryImplTest {

    private val AtsGasPrice = BigInteger.valueOf(100_000_000_000)
    private val EthGasPrice = BigInteger.valueOf(20_000_000_000)

    private val ETH = "ETH"
    private val ATS = "ATS"
    private val web3J = mockk<Web3j>()
    private val ensResolver = mockk<EnsResolver>()
    private val web3Js: Map<String, Web3j> = mapOf(Pair(ETH, web3J))
    private val gasPrice: Map<String, BigInteger> = mapOf(Pair(ETH, EthGasPrice), Pair(ATS, AtsGasPrice))

    private val blockchainRegularAccountRepository: BlockchainRegularAccountRepositoryImpl =
        BlockchainRegularAccountRepositoryImpl(web3Js, gasPrice, ensResolver)

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
        blockchainRegularAccountRepository.refreshBalances(listOf(Pair(ETH, "0x9866208bea68b10f04697c00b891541a305df851")))
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
        blockchainRegularAccountRepository.refreshBalances(listOf(Pair(ETH, "0x9866208bea68b10f04697c00b891541a305df851")))
            .test()
            .await()
            .assertError(error)
    }

    @Test
    fun `get transaction for main tx costs success test`() {
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val ethEstimateGas = EthEstimateGas()
        ethEstimateGas.result = "0x1"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethEstimateGas(any()).flowable() } returns Flowable.just(ethEstimateGas)
        val ethCostPayload = blockchainRegularAccountRepository.getTransactionCosts(ETH, Int.InvalidIndex, "from", "to", BigDecimal.TEN)
        ethCostPayload.test()
            .assertComplete()
            .assertValue {
            it.gasPrice == BigDecimal(20)

            }
    }

    @Test
    fun `get transaction costs for asset tx success test`() {
        val ethCostPayload = blockchainRegularAccountRepository.getTransactionCosts(ETH, 1, "from", "to", BigDecimal.TEN)
        ethCostPayload.test()
            .assertComplete()
            .assertValue {
                it.gasLimit == Operation.TRANSFER_ERC20.gasLimit
            }
    }

    @Test
    fun `send transaction success test`() {
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val sendTransaction = EthSendTransaction()
        sendTransaction.result = "0x2"
        val netVersion = NetVersion()
        netVersion.result = "124"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.just(sendTransaction)
        every { web3J.netVersion().flowable() } returns Flowable.just(netVersion)
        blockchainRegularAccountRepository.transferNativeCoin(ETH, 1, TransactionPayload("address", "0x2313"))
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
        val netVersion = NetVersion()
        netVersion.result = "124"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.error(error)
        every { web3J.netVersion().flowable() } returns Flowable.just(netVersion)
        blockchainRegularAccountRepository.transferNativeCoin(ETH, 1, TransactionPayload("address", "0x2313"))
            .test()
            .assertError(error)
    }

    @Test
    fun `resolve normal name test`() {
        blockchainRegularAccountRepository.resolveENS("tom")
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it == "tom"
            }
    }

    @Test
    fun `resolve ens name test`() {
        every { ensResolver.resolve(any()) } returns "tom"
        blockchainRegularAccountRepository.resolveENS("tom.eth")
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it == "tom"
            }
    }

    @Test
    fun `reverse resolver ens`() {
        every { ensResolver.reverseResolve(any()) } returns "tom.eth"
        blockchainRegularAccountRepository.reverseResolveENS("0x12332423")
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it == "tom.eth"
            }
    }

    @Test
    fun `to gwei conversion test`() {
        val result = blockchainRegularAccountRepository.toGwei(BigDecimal.ONE)
        assertEquals(result, BigDecimal.valueOf(1000000000))
    }
}
