package minerva.android.walletmanager.repository

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.GasPrice
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.Price
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.model.PendingTransaction
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepositoryImpl
import minerva.android.walletmanager.utils.RxTest
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.repository.transaction.TransactionRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class TransactionRepositoryTest : RxTest() {

    private val walletConfigManager: WalletConfigManager = mock()
    private val blockchainRegularAccountRepository: BlockchainRegularAccountRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val webSocketRepositoryImpl: WebSocketRepositoryImpl = mock()
    private val cryptoApi: CryptoApi = mock()
    private val tokenManager: TokenManager = mock()

    private val repository =
        TransactionRepositoryImpl(
            blockchainRegularAccountRepository,
            walletConfigManager,
            cryptoApi,
            localStorage,
            webSocketRepositoryImpl,
            tokenManager
        )

    @Before
    fun initialize() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(walletConfigManager.getWalletConfig()) doReturn DataProvider.walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @Test
    fun `refresh balances test success`() {
        whenever(blockchainRegularAccountRepository.refreshBalances(any()))
            .thenReturn(Single.just(listOf(Pair("address", BigDecimal.ONE))))
        whenever(
            cryptoApi.getMarkets(
                any(),
                any()
            )
        ).thenReturn(Single.just(Markets(ethPrice = Price(value = 1.0))))
        repository.refreshBalances().test()
            .assertComplete()
            .assertValue {
                it["address"]?.cryptoBalance == BigDecimal.ONE
            }
    }

    @Test
    fun `refresh balances test error`() {
        val error = Throwable()
        whenever(blockchainRegularAccountRepository.refreshBalances(any())).thenReturn(
            Single.error(
                error
            )
        )
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.error(error))
        repository.refreshBalances().test()
            .assertError(error)
    }

    @Test
    fun `refresh balances test when crypto api returns error success`() {
        val error = Throwable()
        whenever(blockchainRegularAccountRepository.refreshBalances(any()))
            .thenReturn(Single.just(listOf(Pair("address", BigDecimal.ONE))))
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.error(error))
        repository.refreshBalances().test()
            .assertComplete()
            .assertValue {
                it["address"]?.cryptoBalance == BigDecimal.ONE
            }
    }

    @Test
    fun `send transaction success with resolved ENS test when there isno  wss uri available`() {
        NetworkManager.initialize(listOf(Network(short = "ATS", httpRpc = "httpRpc", wsRpc = "")))
        whenever(blockchainRegularAccountRepository.transferNativeCoin(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PendingTransaction(
                        index = 1,
                        txHash = "hash",
                        network = "ATS"
                    )
                )
            )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.just(
                "didi.eth"
            )
        )
        repository.transferNativeCoin(
            "",
            1,
            Transaction(
                "address",
                "privKey",
                "publicKey",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigInteger.ONE
            )
        )
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction success with resolved ENS test when there is wss uri available`() {
        NetworkManager.initialize(
            listOf(
                Network(
                    short = "ATS",
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri"
                )
            )
        )
        whenever(blockchainRegularAccountRepository.transferNativeCoin(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PendingTransaction(
                        index = 1,
                        txHash = "hash",
                        network = "ATS"
                    )
                )
            )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.just(
                "didi.eth"
            )
        )
        repository.transferNativeCoin(
            "",
            1,
            Transaction(
                "address",
                "privKey",
                "publicKey",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigInteger.ONE
            )
        )
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction success with not resolved ENS test when there is wss uri available`() {
        NetworkManager.initialize(
            listOf(
                Network(
                    short = "ATS",
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri"
                )
            )
        )
        whenever(blockchainRegularAccountRepository.transferNativeCoin(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PendingTransaction(
                        index = 1,
                        txHash = "hash",
                        network = "ATS"
                    )
                )
            )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.error(
                Throwable("No ENS")
            )
        )
        repository.transferNativeCoin(
            "",
            1,
            Transaction(
                "address",
                "privKey",
                "publicKey",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigInteger.ONE
            )
        )
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction error test`() {
        val error = Throwable()
        whenever(
            blockchainRegularAccountRepository.transferNativeCoin(
                any(),
                any(),
                any()
            )
        ).thenReturn(Single.error(error))
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        repository.transferNativeCoin(
            "",
            1,
            Transaction(
                "address",
                "privKey",
                "publicKey",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigInteger.ONE
            )
        )
            .test()
            .assertError(error)
    }

    @Test
    fun `get assets balances complete test`() {
        NetworkManager.initialize(DataProvider.networks)
        whenever(walletConfigManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        whenever(
            blockchainRegularAccountRepository.refreshTokenBalance(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Observable.just(Pair("privateKey1", BigDecimal.TEN))
        )
        repository.apply {
            refreshTokenBalance()
                .test()
                .assertComplete()
        }
    }

    @Test
    fun `get asset balances when values are empty and there are no assets needed test`() {
        val error = Throwable()
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(
            0,
            accounts = emptyList()
        )
        whenever(
            blockchainRegularAccountRepository.refreshTokenBalance(
                any(),
                any(),
                any(),
                any()
            )
        ) doReturn Observable.error(
            error
        )
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed())
        repository.apply {
            refreshTokenBalance()
                .test()
                .assertComplete()
                .assertValue {
                    it.isEmpty()
                }
        }
    }

    @Test
    fun `make ERC20 transfer with ENS resolved success test`() {
        whenever(blockchainRegularAccountRepository.transferERC20Token(any(), any())).thenReturn(
            Completable.complete()
        )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.just(
                "didi.eth"
            )
        )
        repository.transferERC20Token("", Transaction())
            .test()
            .assertComplete()
    }

    @Test
    fun `make ERC20 transfer with not ENS resolved success test`() {
        whenever(blockchainRegularAccountRepository.transferERC20Token(any(), any())).thenReturn(
            Completable.complete()
        )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        repository.transferERC20Token("", Transaction()).test().assertComplete()
    }

    @Test
    fun `make ERC20 transfer error test`() {
        val error = Throwable()
        whenever(blockchainRegularAccountRepository.transferERC20Token(any(), any())).thenReturn(
            Completable.error(error)
        )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.just(
                "didi.eth"
            )
        )
        repository.transferERC20Token("", Transaction()).test().assertError(error)
    }

    @Test
    fun `resolve ens test`() {
        whenever(blockchainRegularAccountRepository.resolveENS(any())) doReturn Single.just("tom")
        repository.resolveENS("tom.eth")
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it == "tom"
            }
    }

    @Test
    fun `resolve ens error test`() {
        val error = Throwable()
        whenever(blockchainRegularAccountRepository.resolveENS(any())) doReturn Single.error(error)
        repository.resolveENS("tom.eth")
            .test()
            .assertError(error)
    }

    @Test
    fun `load recipients test`() {
        whenever(localStorage.getRecipients()) doReturn listOf(Recipient(ensName = "tom"))
        val result = repository.loadRecipients()
        assertEquals(result, listOf(Recipient(ensName = "tom")))
    }

    @Test
    fun `get value test`() {
        whenever(walletConfigManager.getAccount(any())) doReturn Account(id = 2)
        val result = repository.getAccount(2)
        assertEquals(result?.id, 2)
    }

    @Test
    fun `get transactions success test`() {
        whenever(blockchainRegularAccountRepository.getTransactions(any())).thenReturn(
            Single.just(
                listOf(Pair("123", "hash"))
            )
        )
        whenever(localStorage.getPendingAccounts()).thenReturn(
            listOf(
                PendingAccount(
                    1,
                    txHash = "123"
                )
            )
        )
        repository.getTransactions()
            .test()
            .assertNoErrors()
            .assertValue {
                it[0].blockHash == "hash"
            }
    }

    @Test
    fun `get transactions error test`() {
        val error = Throwable()
        whenever(blockchainRegularAccountRepository.getTransactions(any())).thenReturn(
            Single.error(
                error
            )
        )
        whenever(localStorage.getPendingAccounts()).thenReturn(
            listOf(
                PendingAccount(
                    1,
                    txHash = "123"
                )
            )
        )
        repository.getTransactions()
            .test()
            .assertError(error)
    }

    @Test
    fun `subscribe to pending transactions success test`() {
        val pendingAccount =
            PendingAccount(1, network = "abc", txHash = "hash", senderAddress = "sender")
        whenever(localStorage.getPendingAccounts()).thenReturn(listOf(pendingAccount))
        whenever(webSocketRepositoryImpl.subscribeToExecutedTransactions(any(), any())).thenReturn(
            Flowable.just(
                ExecutedTransaction(
                    "hash",
                    "sender"
                )
            )
        )
        repository.subscribeToExecutedTransactions(1)
            .test()
            .assertNoErrors()
            .assertValue {
                it.txHash == "hash"
            }
    }

    @Test
    fun `subscribe to pending transactions error test`() {
        val pendingAccount =
            PendingAccount(1, network = "abc", txHash = "hash", senderAddress = "sender")
        val error = Throwable()
        whenever(localStorage.getPendingAccounts()).thenReturn(listOf(pendingAccount))
        whenever(webSocketRepositoryImpl.subscribeToExecutedTransactions(any(), any())).thenReturn(
            Flowable.error(error)
        )
        repository.subscribeToExecutedTransactions(1)
            .test()
            .assertError(error)
    }

    @Test
    fun `should open wss connection when there is only one pending account test success`() {
        val pendingAccount =
            PendingAccount(1, network = "ats", txHash = "hash", senderAddress = "sender")
        whenever(localStorage.getPendingAccounts()).thenReturn(listOf(pendingAccount))
        val result = repository.shouldOpenNewWssConnection(1)
        assertEquals(true, result)
    }

    @Test
    fun `should open wss connection when there are more than one pending accounts with the same network test success`() {
        val pendingAccountAts1 =
            PendingAccount(1, network = "ats", txHash = "hash", senderAddress = "sender")
        val pendingAccountAts2 =
            PendingAccount(2, network = "ats", txHash = "hash", senderAddress = "sender")

        whenever(localStorage.getPendingAccounts()).thenReturn(
            listOf(
                pendingAccountAts1,
                pendingAccountAts2
            )
        )
        val result = repository.shouldOpenNewWssConnection(2)
        assertEquals(false, result)
    }

    @Test
    fun `should open wss connection when there are two pending accounts with the different network test`() {
        val pendingAccount =
            PendingAccount(1, network = "ats", txHash = "hash", senderAddress = "sender")
        val pendingAccount1 =
            PendingAccount(2, network = "ats", txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa =
            PendingAccount(3, network = "poa", txHash = "hash", senderAddress = "sender")

        whenever(localStorage.getPendingAccounts()).thenReturn(
            listOf(
                pendingAccount,
                pendingAccount1,
                pendingAccountPoa
            )
        )
        val result = repository.shouldOpenNewWssConnection(3)
        assertEquals(true, result)
    }

    @Test
    fun `should not open wss connection when there are two pending accounts with the same network test`() {
        val pendingAccount =
            PendingAccount(1, network = "ats", txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa1 =
            PendingAccount(2, network = "poa", txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa2 =
            PendingAccount(3, network = "poa", txHash = "hash", senderAddress = "sender")

        whenever(localStorage.getPendingAccounts()).thenReturn(
            listOf(
                pendingAccount,
                pendingAccountPoa1,
                pendingAccountPoa2
            )
        )
        val result = repository.shouldOpenNewWssConnection(3)
        assertEquals(false, result)
    }

    @Test
    fun `should not open wss connection when there are two pending accounts with the same network and the first one is already opened test`() {
        val pendingAccount =
            PendingAccount(1, network = "ats", txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa1 =
            PendingAccount(2, network = "poa", txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa2 =
            PendingAccount(3, network = "poa", txHash = "hash", senderAddress = "sender")
        val pendingAccountEth =
            PendingAccount(4, network = "eth", txHash = "hash", senderAddress = "sender")

        whenever(localStorage.getPendingAccounts()).thenReturn(
            listOf(
                pendingAccount,
                pendingAccountPoa1,
                pendingAccountPoa2,
                pendingAccountEth
            )
        )
        val result = repository.shouldOpenNewWssConnection(1)
        assertEquals(false, result)
    }

    @Test
    fun `should open wss connection test failed when no pending accounts`() {
        whenever(localStorage.getPendingAccounts()).thenReturn(emptyList())
        val result = repository.shouldOpenNewWssConnection(7)
        assertEquals(false, result)
    }

    @Test
    fun `get transaction costs success when there is no gas price from oracle`() {
        NetworkManager.initialize(
            listOf(
                Network(
                    short = "ATS",
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri",
                    gasPriceOracle = ""
                )
            )
        )
        whenever(
            blockchainRegularAccountRepository.getTransactionCosts(
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(null)
            )
        ).doReturn(
            Single.just(TransactionCostPayload(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN))
        )
        repository.getTransactionCosts("ATS", 1, "from", "to", BigDecimal.TEN)
            .test()
            .assertComplete()
            .assertValue {
                it.gasPrice == BigDecimal.TEN
            }
    }

    @Test
    fun `get transaction costs success when there is gas price from oracle available`() {
        NetworkManager.initialize(
            listOf(
                Network(
                    short = "ATS",
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri",
                    gasPriceOracle = "url"
                )
            )
        )
        whenever(
            cryptoApi.getGasPrice(
                any(),
                any()
            )
        ).thenReturn(Single.just(GasPrice(BigDecimal.TEN)))
        whenever(
            blockchainRegularAccountRepository.getTransactionCosts(
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(BigDecimal.ONE)
            )
        )
            .doReturn(
                Single.just(
                    TransactionCostPayload(
                        BigDecimal.TEN,
                        BigInteger.ONE,
                        BigDecimal.TEN
                    )
                )
            )
        repository.getTransactionCosts("ATS", 1, "from", "to", BigDecimal.TEN)
            .test()
            .assertComplete()
            .assertValue {
                it.gasPrice == BigDecimal.TEN
            }
    }

    @Test
    fun `get transaction costs error when there is no gas price from oracle`() {
        val error = Throwable()
        NetworkManager.initialize(
            listOf(
                Network(
                    short = "ATS",
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri"
                )
            )
        )
        whenever(
            blockchainRegularAccountRepository.getTransactionCosts(
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(null)
            )
        )
            .doReturn(Single.error(error))
        repository.getTransactionCosts("ATS", 1, "from", "to", BigDecimal.TEN)
            .test()
            .assertError(error)
    }

    @Test
    fun `get transaction costs error when there is gas price from oracle available`() {
        val error = Throwable()
        NetworkManager.initialize(
            listOf(
                Network(
                    short = "ATS",
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri",
                    gasPriceOracle = "url"
                )
            )
        )
        whenever(cryptoApi.getGasPrice(any(), any())).thenReturn(Single.error(error))
        whenever(
            blockchainRegularAccountRepository.getTransactionCosts(
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(null)
            )
        )
            .doReturn(
                Single.just(
                    TransactionCostPayload(
                        BigDecimal.ONE,
                        BigInteger.ONE,
                        BigDecimal.TEN
                    )
                )
            )
        repository.getTransactionCosts("ATS", 1, "from", "to", BigDecimal.TEN)
            .test()
            .assertComplete()
            .assertValue {
                it.gasPrice == BigDecimal.ONE
            }
    }

    @Test
    fun `is address valid success`() {
        whenever(blockchainRegularAccountRepository.isAddressValid(any())).thenReturn(true)
        val result = repository.isAddressValid("0x12345")
        assertEquals(true, result)
    }

    @Test
    fun `is address valid false`() {
        whenever(blockchainRegularAccountRepository.isAddressValid(any())).thenReturn(false)
        val result = repository.isAddressValid("123455")
        assertEquals(false, result)
    }
}