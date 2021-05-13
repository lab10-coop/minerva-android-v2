package minerva.android.walletmanager.repository

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.*
import minerva.android.blockchainprovider.model.ExecutedTransaction
import minerva.android.blockchainprovider.model.PendingTransaction
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.repository.regularAccont.BlockchainRegularAccountRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepositoryImpl
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.transaction.TransactionRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.DataProvider
import minerva.android.walletmanager.utils.RxTest
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
        whenever(walletConfigManager.getWalletConfig()).thenReturn(DataProvider.walletConfig)
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @Test
    fun `refresh balances test success`() {
        whenever(blockchainRegularAccountRepository.refreshBalances(any()))
            .thenReturn(Single.just(listOf(Pair("address1", BigDecimal.ONE))))
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.just(Markets(ethFiatPrice = FiatPrice(eur = 1.0))))
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")

        repository.refreshBalances().test()
            .assertComplete()
            .assertValue {
                it["address1"]?.cryptoBalance == BigDecimal.ONE
            }
    }

    @Test
    fun `refresh balances test error`() {
        val error = Throwable()
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")
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
            .thenReturn(Single.just(listOf(Pair("address1", BigDecimal.ONE))))
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.error(error))
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")
        repository.refreshBalances().test()
            .assertComplete()
            .assertValue {
                it["address1"]?.cryptoBalance == BigDecimal.ONE
            }
    }

    @Test
    fun `send transaction success with resolved ENS test when there isno  wss uri available`() {
        NetworkManager.initialize(listOf(Network(chainId = 1, httpRpc = "httpRpc", wsRpc = "")))
        whenever(blockchainRegularAccountRepository.transferNativeCoin(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PendingTransaction(
                        index = 1,
                        txHash = "hash",
                        chainId = 1
                    )
                )
            )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.just(
                "didi.eth"
            )
        )
        repository.transferNativeCoin(
            0,
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
                    chainId = 1,
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
                        chainId = 1
                    )
                )
            )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.just(
                "didi.eth"
            )
        )
        repository.transferNativeCoin(
            0,
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
                    chainId = 1,
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
                        chainId = 1
                    )
                )
            )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.error(
                Throwable("No ENS")
            )
        )
        repository.transferNativeCoin(
            0,
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
            0,
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
    fun `make ERC20 transfer with ENS resolved success test`() {
        whenever(blockchainRegularAccountRepository.transferERC20Token(any(), any())).thenReturn(
            Completable.complete()
        )
        whenever(blockchainRegularAccountRepository.reverseResolveENS(any())).thenReturn(
            Single.just(
                "didi.eth"
            )
        )
        repository.transferERC20Token(0, Transaction())
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
        repository.transferERC20Token(0, Transaction()).test().assertComplete()
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
        repository.transferERC20Token(0, Transaction()).test().assertError(error)
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
            PendingAccount(1, chainId = 11, txHash = "hash", senderAddress = "sender")
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
            PendingAccount(1, chainId = 11, txHash = "hash", senderAddress = "sender")
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
            PendingAccount(1, chainId = 11, txHash = "hash", senderAddress = "sender")
        whenever(localStorage.getPendingAccounts()).thenReturn(listOf(pendingAccount))
        val result = repository.shouldOpenNewWssConnection(1)
        assertEquals(true, result)
    }

    @Test
    fun `should open wss connection when there are more than one pending accounts with the same network test success`() {
        val pendingAccountAts1 =
            PendingAccount(1, chainId = 11, txHash = "hash", senderAddress = "sender")
        val pendingAccountAts2 =
            PendingAccount(2, chainId = 11, txHash = "hash", senderAddress = "sender")

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
            PendingAccount(1, chainId = 11, txHash = "hash", senderAddress = "sender")
        val pendingAccount1 =
            PendingAccount(2, chainId = 11, txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa =
            PendingAccount(3, chainId = 12, txHash = "hash", senderAddress = "sender")

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
            PendingAccount(1, chainId = 11, txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa1 =
            PendingAccount(2, chainId = 12, txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa2 =
            PendingAccount(3, chainId = 12, txHash = "hash", senderAddress = "sender")

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
            PendingAccount(1, chainId = 11, txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa1 =
            PendingAccount(2, chainId = 12, txHash = "hash", senderAddress = "sender")
        val pendingAccountPoa2 =
            PendingAccount(3, chainId = 12, txHash = "hash", senderAddress = "sender")
        val pendingAccountEth =
            PendingAccount(4, chainId = 13, txHash = "hash", senderAddress = "sender")

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
                    chainId = 1,
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri",
                    gasPriceOracle = ""
                )
            )
        )
        whenever(
            blockchainRegularAccountRepository.getTransactionCosts(any(), eq(null))
        ).doReturn(
            Single.just(TransactionCostPayload(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN))
        )
        repository.getTransactionCosts(TxCostPayload(TransferType.COIN_TRANSFER, chainId = 1))
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
                    chainId = 1,
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri",
                    gasPriceOracle = "url"
                )
            )
        )
        whenever(cryptoApi.getGasPrice(any(), any())).thenReturn(
            Single.just(
                GasPrices("code", TransactionSpeed(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN))
            )
        )
        whenever(
            blockchainRegularAccountRepository.getTransactionCosts(any(), any())
        ).doReturn(Single.just(TransactionCostPayload(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN)))
        whenever(blockchainRegularAccountRepository.fromWei(any())).thenReturn(BigDecimal.TEN)
        repository.getTransactionCosts(TxCostPayload(TransferType.COIN_TRANSFER, chainId = 1))
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
                    chainId = 1,
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri"
                )
            )
        )
        whenever(blockchainRegularAccountRepository.getTransactionCosts(any(), eq(null))).doReturn(Single.error(error))
        repository.getTransactionCosts(TxCostPayload(TransferType.COIN_TRANSFER, chainId = 1))
            .test()
            .assertError(error)
    }

    @Test
    fun `get transaction costs error when there is gas price from oracle available`() {
        val error = Throwable()
        NetworkManager.initialize(
            listOf(
                Network(
                    chainId = 1,
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri",
                    gasPriceOracle = "url"
                )
            )
        )
        whenever(cryptoApi.getGasPrice(any(), any())).thenReturn(Single.error(error))
        whenever(blockchainRegularAccountRepository.getTransactionCosts(any(), eq(null)))
            .doReturn(
                Single.just(
                    TransactionCostPayload(
                        BigDecimal.ONE,
                        BigInteger.ONE,
                        BigDecimal.TEN
                    )
                )
            )
        repository.getTransactionCosts(TxCostPayload(TransferType.COIN_TRANSFER, chainId = 1))
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

    @Test
    fun `Checking token icon updates`() {
        whenever(tokenManager.checkMissingTokensDetails()).thenReturn(Completable.complete())
        repository.checkMissingTokensDetails()
        verify(tokenManager, times(1)).checkMissingTokensDetails()
    }

    @Test
    fun `get account by address test`() {
        whenever(walletConfigManager.getWalletConfig()).thenReturn(
            WalletConfig(
                version = 1,
                accounts = listOf(Account(1, address = "address"))
            )
        )
        whenever(blockchainRegularAccountRepository.toChecksumAddress(any())).thenReturn("address")
        val result = repository.getAccountByAddress("address")
        assertEquals(result?.address, "address")
    }

    @Test
    fun `get eur rate test`() {
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.just(Markets(ethFiatPrice = FiatPrice(eur = 1.2))))
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")
        repository.getCoinFiatRate(2)
            .test()
            .assertComplete()
            .assertValue {
                it == 0.0
            }
    }

    @Test
    fun `get eur rate for eth test`() {
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.just(Markets(ethFiatPrice = FiatPrice(eur = 1.2))))
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")
        repository.getCoinFiatRate(1)
            .test()
            .assertComplete()
            .assertValue {
                it == 1.2
            }
    }

    @Test
    fun `to ether conversions test`() {
        whenever(blockchainRegularAccountRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        val result = repository.toEther(BigDecimal.TEN)
        assertEquals(result, BigDecimal.TEN)
    }

    @Test
    fun `send transaction success`() {
        whenever(blockchainRegularAccountRepository.sendWalletConnectTransaction(any(), any())).thenReturn(Single.just("txHash"))
        repository.sendTransaction(111, Transaction(address = "address"))
            .test()
            .assertComplete()
            .assertValue {
                it == "txHash"
            }
    }

    @Test
    fun `send transaction error`() {
        val error = Throwable()
        whenever(blockchainRegularAccountRepository.sendWalletConnectTransaction(any(), any())).thenReturn(Single.error(error))
        repository.sendTransaction(111, Transaction(address = "address"))
            .test()
            .assertError(error)
    }

    @Test
    fun `Checking refreshing token balances success`() {
        val accountTokens = listOf(
            AccountToken(ERC20Token(3, "one", address = "0x01"), BigDecimal.TEN),
            AccountToken(ERC20Token(3, "tow", address = "0x02"), BigDecimal.TEN)
        )
        whenever(tokenManager.refreshTokensBalances(any())).thenReturn(Single.just(Pair("privateKey", accountTokens)))
        whenever(tokenManager.getTokensRate(any())).thenReturn(Completable.complete())

        repository.refreshTokensBalances().test().assertComplete().assertValue {
            it.size == 1
            it["privateKey"]?.size == 2
        }
    }

    @Test
    fun `Checking refreshing token balances error`() {
        val error = Throwable("error")
        whenever(tokenManager.refreshTokensBalances(any())).thenReturn(Single.error(error))
        repository.refreshTokensBalances().test().assertError(error)
    }


    @Test
    fun `Check refreshing tokens list success`() {
        val tokensList = listOf(
            ERC20Token(3, "Token01", address = "0x0N3"),
            ERC20Token(3, "Token02", address = "0xTW0"),
            ERC20Token(3, "Token03", address = "0xTHR33")
        )

        val tokensMap = mapOf(Pair(3, tokensList))
        val updatedTokensMap = Pair(true, tokensMap)

        whenever(tokenManager.downloadTokensList(any())).thenReturn(Single.just(tokensList))
        whenever(tokenManager.sortTokensByChainId(any())).thenReturn(tokensMap)
        whenever(tokenManager.mergeWithLocalTokensList(any())).thenReturn(updatedTokensMap)
        whenever(tokenManager.updateTokenIcons(any(), any())).thenReturn(
            Single.just(updatedTokensMap),
            Single.error(Throwable("Stop thread"))
        )
        whenever(tokenManager.saveTokens(any(), any())).thenReturn(Single.just(true), Single.error(Throwable("Stop thread")))

        repository.refreshTokensList().test().assertComplete().assertValue { it }
        repository.refreshTokensList().test().assertComplete().assertValue { !it }
    }

    @Test
    fun `Check refreshing tokens list fail`() {
        val error = Throwable("error")
        val tokensMap = mapOf(Pair(3, listOf<ERC20Token>()))
        val updatedTokensMap = Pair(true, tokensMap)
        whenever(tokenManager.downloadTokensList(any())).thenReturn(Single.error(error))
        whenever(tokenManager.sortTokensByChainId(any())).thenReturn(tokensMap)
        whenever(tokenManager.mergeWithLocalTokensList(any())).thenReturn(updatedTokensMap)
        whenever(tokenManager.updateTokenIcons(any(), any())).thenReturn(Single.just(updatedTokensMap))

        repository.refreshTokensList().test().assertError(error)
    }

    @Test
    fun `Check getting current tokens rate` () {
        val error = Throwable("Error-303")
        whenever(tokenManager.getTokensRate(any())).thenReturn(Completable.complete(), Completable.error(error))

        repository.getTokensRate().test().assertComplete()
        repository.getTokensRate().test().assertError(error)
        verify(tokenManager, times(2)).getTokensRate(any())
    }
}