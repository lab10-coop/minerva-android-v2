package minerva.android

import io.mockk.every
import io.mockk.mockk
import io.reactivex.Flowable
import minerva.android.blockchainprovider.defs.BlockchainTransactionType
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.model.TransactionPayload
import minerva.android.blockchainprovider.model.TxCostData
import minerva.android.blockchainprovider.repository.freeToken.FreeTokenRepository
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepositoryImpl
import org.junit.Test
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals


class BlockchainRegularAccountRepositoryImplTest : RxTest() {

    private val AtsGasPrice = BigInteger.valueOf(100_000_000_000)
    private val EthGasPrice = BigInteger.valueOf(20_000_000_000)

    private val ETH = 2
    private val ATS = 1
    private val web3J = mockk<Web3j>()
    private val ensResolver = mockk<EnsResolver>()
    private val freeTokenRepository = mockk<FreeTokenRepository>()
    private val web3Js: Map<Int, Web3j> = mapOf(Pair(ETH, web3J))
    private val gasPrice: Map<Int, BigInteger> =
        mapOf(Pair(ETH, EthGasPrice), Pair(ATS, AtsGasPrice))

    private val repository: BlockchainRegularAccountRepositoryImpl =
        BlockchainRegularAccountRepositoryImpl(web3Js, gasPrice, ensResolver, freeTokenRepository)

    @Test
    fun `refresh balance success`() {
        val ethBalance = EthGetBalance()
        ethBalance.result = "0x1"
        every { web3J.ethGetBalance(any(), any()).flowable() } returns Flowable.just(ethBalance)
        repository.refreshBalances(
            listOf(Pair(ETH, "0x9866208bea68b10f04697c00b891541a305df851"))
        )
            .test()
            .await()
            .assertValue {
                it[0].second == "0x9866208bea68b10f04697c00b891541a305df851"
            }
    }

    @Test
    fun `refresh balance error`() {
        val error = Throwable()
        val ethBalance = EthGetBalance()
        ethBalance.result = "0x1"
        every { web3J.ethGetBalance(any(), any()).flowable() } returns Flowable.error(error)
        repository.refreshBalances(
            listOf(Pair(ETH, "0x9866208bea68b10f04697c00b891541a305df851"))
        )
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
        val ethCall = EthCall()
        ethCall.result = "0x1"
        every { web3J.ethGetTransactionCount(any(), any()).flowable() } returns Flowable.just(transactionCount)
        every { web3J.ethEstimateGas(any()).flowable() } returns Flowable.just(ethEstimateGas)
        every { web3J.ethCall(any(), any()).flowable() } returns Flowable.just(ethCall)
        val ethCostPayload =
            repository.getTransactionCosts(
                TxCostData(BlockchainTransactionType.COIN_TRANSFER, from = "from", amount = BigDecimal.TEN, chainId = ETH),
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
    fun `resolve normal name test`() {
        repository.resolveENS("tom")
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
        repository.resolveENS("tom.eth")
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
        repository.reverseResolveENS("0x12332423")
            .test()
            .await()
            .assertComplete()
            .assertValue {
                it == "tom.eth"
            }
    }

    @Test
    fun `to gwei conversion test`() {
        val result = repository.toGwei(BigDecimal.ONE)
        assertEquals(result, BigDecimal.valueOf(1000000000))
    }

    @Test
    fun `get transaction cost in ether`() {
        val result = repository.getTransactionCostInEth(BigDecimal.ONE, BigDecimal.TEN).toDouble()
        assertEquals(0.0, result)
    }

    @Test
    fun `is address with checksum valid success test`() {
        val result = repository.isAddressValid("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359")
        assertEquals(true, result)
    }

    @Test
    fun `is address with no checksum and with random big letters invalid success test`() {
        val result = repository.isAddressValid("0x9866208bea68B10f04697c00b891541a305Df851")
        assertEquals(false, result)
    }

    @Test
    fun `is address with no checksum but with all small letters success test`() {
        val result = repository.isAddressValid("0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359")
        assertEquals(true, result)
    }

    @Test
    fun `is address valid fail test`() {
        val result = repository.isAddressValid("address")
        assertEquals(false, result)
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
    fun `get block number success`() {
        val ethBlockNumber = EthBlockNumber()
        ethBlockNumber.result = "0x1"
        every { web3J.ethBlockNumber().flowable() } returns Flowable.just(ethBlockNumber)
        repository.getCurrentBlockNumber(ETH)
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `get block number error`() {
        val error = Throwable()
        every { web3J.ethBlockNumber().flowable() } returns Flowable.error(error)
        repository.getCurrentBlockNumber(ETH)
            .test()
            .assertError(error)
    }

    @Test
    fun `to address checksum test`() {
        val result = repository.toChecksumAddress("0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359")
        assertEquals("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359", result)
    }

    @Test
    fun `Getting Free ATS request`() {
        every { freeTokenRepository.getFreeATS("some address") } returns "txHash: 0xCookie"
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

    @Test
    fun `to ether conversion test`() {
        val result = repository.toEther(BigDecimal.valueOf(1000000000000000000))
        assertEquals(result, BigDecimal.ONE)

        val result2 = repository.toEther(BigDecimal("1"))
        assertEquals(result2, BigDecimal("1E-18"))
    }
}
