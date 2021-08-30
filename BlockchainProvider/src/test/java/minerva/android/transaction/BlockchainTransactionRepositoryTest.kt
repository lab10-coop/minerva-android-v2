package minerva.android.transaction

import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.blockchainprovider.defs.BlockchainTransactionType
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.TokenWithBalance
import minerva.android.blockchainprovider.model.TokenWithError
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.model.TxCostData
import minerva.android.blockchainprovider.repository.ens.ENSRepository
import minerva.android.blockchainprovider.repository.freeToken.FreeTokenRepository
import minerva.android.blockchainprovider.repository.transaction.BlockchainTransactionRepositoryImpl
import minerva.android.blockchainprovider.repository.units.UnitConverter
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class BlockchainTransactionRepositoryTest {

    private val AtsGasPrice = BigInteger.valueOf(100_000_000_000)
    private val EthGasPrice = BigInteger.valueOf(20_000_000_000)
    private val ETH = 2
    private val ATS = 1
    private val web3J = mockk<Web3j>()
    private val web3Js: Map<Int, Web3j> = mapOf(Pair(ETH, web3J))
    private val gasPrice: Map<Int, BigInteger> =
        mapOf(Pair(ETH, EthGasPrice), Pair(ATS, AtsGasPrice))
    private val ensRepository = mockk<ENSRepository>()
    private val unitConverter = mockk<UnitConverter>()
    private val freeTokenRepository = mockk<FreeTokenRepository>()
    private val repository =
        BlockchainTransactionRepositoryImpl(web3Js, gasPrice, unitConverter, ensRepository, freeTokenRepository)


    @Test
    fun `refresh balance success`() {
        val ethBalance = EthGetBalance()
        ethBalance.result = "0x1"
        every { web3J.ethGetBalance(any(), any()).flowable() } returns Flowable.just(ethBalance)
        repository.getCoinBalances(
            listOf(Pair(ETH, "0x9866208bea68b10f04697c00b891541a305df851"))
        )
            .test()
            .await()
            .assertValue { token ->
                token is TokenWithBalance
                token.address == "0x9866208bea68b10f04697c00b891541a305df851"
            }
    }

    @Test
    fun `refresh balance error`() {
        val error = Throwable("Balance Error")
        val ethBalance = EthGetBalance()
        ethBalance.result = "0x1"
        every { web3J.ethGetBalance(any(), any()).flowable() } returns Flowable.error(error)
        repository.getCoinBalances(listOf(Pair(ETH, "0x9866208bea68b10f04697c00b891541a305df851")))
            .test()
            .await()
            .assertNoErrors()
            .assertValue { token ->
                token as TokenWithError
                token.error.message == "Balance Error"
            }
    }

    @Test
    fun `get transaction for main tx costs success test`() {
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val ethEstimateGas = EthEstimateGas()
        ethEstimateGas.result = "0x1"
        val ethCall = EthCall()
        ethCall.result = "0x1"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethEstimateGas(any()).flowable() } returns Flowable.just(ethEstimateGas)
        every { web3J.ethCall(any(), any()).flowable() } returns Flowable.just(ethCall)
        every { ensRepository.resolveENS(any()) } returns Single.just("test")
        every { unitConverter.toEther(any()) } returns BigDecimal.TEN
        val ethCostPayload =
            repository.getTransactionCosts(
                TxCostData(
                    BlockchainTransactionType.COIN_TRANSFER,
                    from = "from",
                    amount = BigDecimal.TEN,
                    chainId = ETH
                ),
                BigDecimal.TEN
            )
        ethCostPayload.test()
            .assertComplete()
            .assertValue {
                it.gasLimit == BigInteger.ONE
            }
    }

    @Test
    fun `get transaction costs for asset tx success test`() {
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val ethEstimateGas = EthEstimateGas()
        ethEstimateGas.result = "0x1"
        val ethCall = EthCall()
        ethCall.result = "0x1"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethEstimateGas(any()).flowable() } returns Flowable.just(ethEstimateGas)
        every { web3J.ethCall(any(), any()).flowable() } returns Flowable.just(ethCall)
        every { ensRepository.resolveENS(any()) } returns Single.just("0x12")
        every { unitConverter.toEther(any()) } returns BigDecimal.TEN
        val ethCostPayload =
            repository.getTransactionCosts(
                TxCostData(
                    BlockchainTransactionType.TOKEN_TRANSFER,
                    from = "from",
                    amount = BigDecimal.TEN,
                    to = "0x12345",
                    chainId = ETH
                ),
                BigDecimal.TEN
            )
        ethCostPayload.test()
            .assertComplete()
            .assertValue {
                it.gasLimit == BigInteger.ONE
            }
    }

    @Test
    fun `get transaction costs for safe accounts tx success test`() {
        every { unitConverter.toEther(any()) } returns BigDecimal.TEN
        val ethCostPayload =
            repository.getTransactionCosts(
                TxCostData(
                    BlockchainTransactionType.SAFE_ACCOUNT_COIN_TRANSFER,
                    chainId = 1,
                    from = "from",
                    amount = BigDecimal.TEN
                ),
                BigDecimal.TEN
            )
        ethCostPayload.test()
            .assertComplete()
            .assertValue {
                it.gasLimit == Operation.SAFE_ACCOUNT_TXS.gasLimit
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
        val ethBlockNumber = EthBlockNumber()
        ethBlockNumber.result = "0x1"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.just(sendTransaction)
        every { web3J.netVersion().flowable() } returns Flowable.just(netVersion)
        every { web3J.ethBlockNumber().flowable() } returns Flowable.just(ethBlockNumber)
        repository.transferNativeCoin(ETH, 1, TransactionPayload("address", "0x2313"))
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
        val ethBlockNumber = EthBlockNumber()
        ethBlockNumber.result = "0x1"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.error(error)
        every { web3J.netVersion().flowable() } returns Flowable.just(netVersion)
        every { web3J.ethBlockNumber().flowable() } returns Flowable.just(ethBlockNumber)
        repository.transferNativeCoin(ETH, 1, TransactionPayload("address", "0x2313"))
            .test()
            .assertError(error)
    }

    @Test
    fun `get transaction cost in ether`() {
        every { unitConverter.toEther(any()) } returns BigDecimal.TEN
        val result = repository.getTransactionCostInEth(BigDecimal.ONE, BigDecimal.TEN).toDouble()
        assertEquals(10.0, result)
    }

    @Test
    fun `get transactions success test`() {
        val ethTransaction = EthTransaction()
        val transaction = Transaction()
        transaction.blockHash = "0x1"
        transaction.hash = "0x2"
        ethTransaction.result = transaction

        every { web3J.ethGetTransactionByHash(any()).flowable() } returns Flowable.just(ethTransaction)
        repository.getTransactions(listOf(Pair(ETH, "address")))
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `get transactions error test`() {
        val error = Throwable()
        every { web3J.ethGetTransactionByHash(any()).flowable() } returns Flowable.error(error)
        repository.getTransactions(listOf(Pair(ETH, "address")))
            .test()
            .assertError(error)
    }

    @Test
    fun `Getting Free ATS request`() {
        every { freeTokenRepository.getFreeATS("some address") } returns "txHash"
        repository.getFreeATS("some address")
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `Getting free ATS request error`() {
        val response = "Wrong txHash for: 0xCookie"
        every { freeTokenRepository.getFreeATS("some address") } returns response
        repository.getFreeATS("some address")
            .test()
            .assertErrorMessage(response)
    }

    @Test
    fun `send transaction from wallet connect test success`() {
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val sendTransaction = EthSendTransaction()
        sendTransaction.result = "0x2"
        val netVersion = NetVersion()
        netVersion.result = "124"
        val ethBlockNumber = EthBlockNumber()
        ethBlockNumber.result = "0x1"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.just(sendTransaction)
        every { web3J.netVersion().flowable() } returns Flowable.just(netVersion)
        every { web3J.ethBlockNumber().flowable() } returns Flowable.just(ethBlockNumber)
        repository.sendWalletConnectTransaction(ETH, TransactionPayload("address", "0x2313"))
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction from wallet connect test error`() {
        val error = Throwable()
        val transactionCount = EthGetTransactionCount()
        transactionCount.result = "0x1"
        val sendTransaction = EthSendTransaction()
        sendTransaction.result = "0x2"
        val netVersion = NetVersion()
        netVersion.result = "124"
        val ethBlockNumber = EthBlockNumber()
        ethBlockNumber.result = "0x1"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethSendRawTransaction(any()).flowable() } returns Flowable.error(error)
        every { web3J.netVersion().flowable() } returns Flowable.just(netVersion)
        every { web3J.ethBlockNumber().flowable() } returns Flowable.just(ethBlockNumber)
        repository.sendWalletConnectTransaction(ETH, TransactionPayload("address", "0x2313"))
            .test()
            .assertError(error)
    }

}