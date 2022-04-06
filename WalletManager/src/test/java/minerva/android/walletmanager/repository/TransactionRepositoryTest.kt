package minerva.android.walletmanager.repository

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.apiProvider.api.CryptoApi
import minerva.android.apiProvider.model.FiatPrice
import minerva.android.apiProvider.model.GasPricesFromRpcOverHttp
import minerva.android.apiProvider.model.GasPrices
import minerva.android.apiProvider.model.Markets
import minerva.android.apiProvider.model.gaswatch.TransactionSpeedStats
import minerva.android.blockchainprovider.model.*
import minerva.android.blockchainprovider.repository.ens.ENSRepository
import minerva.android.blockchainprovider.repository.erc1155.ERC1155TokenRepository
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepository
import minerva.android.blockchainprovider.repository.erc721.ERC721TokenRepository
import minerva.android.blockchainprovider.repository.transaction.BlockchainTransactionRepository
import minerva.android.blockchainprovider.repository.units.UnitConverter
import minerva.android.blockchainprovider.repository.validation.ValidationRepository
import minerva.android.blockchainprovider.repository.wss.WebSocketRepositoryImpl
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.minervaprimitives.account.*
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.*
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.asset.AssetBalanceRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MockDataProvider
import minerva.android.walletmanager.utils.MockDataProvider.walletConfig
import minerva.android.walletmanager.utils.RxTest
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TransactionRepositoryTest : RxTest() {

    private val walletConfigManager: WalletConfigManager = mock()
    private val ensRepository: ENSRepository = mock()
    private val unitConverter: UnitConverter = mock()
    private val erC20TokenRepository: ERC20TokenRepository = mock()
    private val erc721TokenRepository: ERC721TokenRepository = mock()
    private val erc1155TokenRepository: ERC1155TokenRepository = mock()
    private val blockchainTransactionRepository: BlockchainTransactionRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val webSocketRepositoryImpl: WebSocketRepositoryImpl = mock()
    private val cryptoApi: CryptoApi = mock()
    private val tokenManager: TokenManager = mock()
    private val validationRepository: ValidationRepository = mock()
    private val assetBalanceRepository: AssetBalanceRepository = mock()

    private val repository =
        TransactionRepositoryImpl(
            blockchainTransactionRepository,
            walletConfigManager,
            erC20TokenRepository,
            erc721TokenRepository,
            erc1155TokenRepository,
            unitConverter,
            ensRepository,
            cryptoApi,
            localStorage,
            webSocketRepositoryImpl,
            tokenManager,
            validationRepository,
            assetBalanceRepository
        )

    @Before
    fun initialize() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(walletConfigManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig)
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @Test
    fun `refresh coin balances test success`() {
        whenever(blockchainTransactionRepository.getCoinBalances(any()))
            .thenReturn(Flowable.just(TokenWithBalance(1, "address1", BigDecimal.ONE)))
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.just(Markets(ethFiatPrice = FiatPrice(eur = 3.0))))
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")
        repository.getCoinBalance().test()
            .assertComplete()
            .assertValue { coin ->
                coin as CoinBalance
                coin.chainId == 1 &&
                        coin.address == "address1" &&
                        coin.balance.cryptoBalance == BigDecimal.ONE
            }
    }

    @Test
    fun `refresh coin balances test error`() {
        val error = Throwable("Balance Error")
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")
        whenever(blockchainTransactionRepository.getCoinBalances(any()))
            .thenReturn(Flowable.just(TokenWithError(1, "address1", error)))
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.error(error))
        repository.getCoinBalance()
            .test()
            .assertNoErrors()
            .assertValue { coin ->
                coin as CoinError
                coin.address == "address1" &&
                        coin.error.message == "Balance Error"
            }
    }

    @Test
    fun `refresh balances test when crypto api returns error success`() {
        val error = Throwable()
        whenever(blockchainTransactionRepository.getCoinBalances(any()))
            .thenReturn(Flowable.just(TokenWithBalance(1, "address1", BigDecimal.ONE)))
        whenever(cryptoApi.getMarkets(any(), any())).thenReturn(Single.error(error))
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")
        repository.getCoinBalance().test()
            .assertComplete()
            .assertValue { coin ->
                coin as CoinBalance
                coin.chainId == 1 &&
                        coin.address == "address1" &&
                        coin.balance.cryptoBalance == BigDecimal.ONE

            }
    }

    @Test
    fun `refresh balances test when balance of coin is 0`() {
        whenever(blockchainTransactionRepository.getCoinBalances(any()))
            .thenReturn(Flowable.just(TokenWithBalance(1, "address1", BigDecimal.ZERO)))
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR")
        repository.getCoinBalance().test()
            .assertComplete()
            .assertValue { coin ->
                coin as CoinBalance
                coin.chainId == 1 &&
                        coin.address == "address1" &&
                        coin.balance.cryptoBalance == BigDecimal.ZERO

            }
    }

    @Test
    fun `send transaction success with resolved ENS test when there isno  wss uri available`() {
        NetworkManager.initialize(listOf(Network(chainId = 1, httpRpc = "httpRpc", wsRpc = "")))
        whenever(blockchainTransactionRepository.transferNativeCoin(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PendingTransaction(
                        index = 1,
                        txHash = "hash",
                        chainId = 1
                    )
                )
            )
        whenever(ensRepository.reverseResolveENS(any())).thenReturn(
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
        whenever(blockchainTransactionRepository.transferNativeCoin(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PendingTransaction(
                        index = 1,
                        txHash = "hash",
                        chainId = 1
                    )
                )
            )
        whenever(ensRepository.reverseResolveENS(any())).thenReturn(
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
        whenever(blockchainTransactionRepository.transferNativeCoin(any(), any(), any()))
            .thenReturn(
                Single.just(
                    PendingTransaction(
                        index = 1,
                        txHash = "hash",
                        chainId = 1
                    )
                )
            )
        whenever(ensRepository.reverseResolveENS(any())).thenReturn(
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
            blockchainTransactionRepository.transferNativeCoin(
                any(),
                any(),
                any()
            )
        ).thenReturn(Single.error(error))
        whenever(ensRepository.reverseResolveENS(any())).thenReturn(
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
        whenever(erC20TokenRepository.transferERC20Token(any(), any())).thenReturn(
            Completable.complete()
        )
        whenever(ensRepository.reverseResolveENS(any())).thenReturn(
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
        whenever(erC20TokenRepository.transferERC20Token(any(), any())).thenReturn(
            Completable.complete()
        )
        whenever(ensRepository.reverseResolveENS(any())).thenReturn(
            Single.error(
                Throwable()
            )
        )
        repository.transferERC20Token(0, Transaction()).test().assertComplete()
    }

    @Test
    fun `make ERC20 transfer error test`() {
        val error = Throwable()
        whenever(erC20TokenRepository.transferERC20Token(any(), any())).thenReturn(
            Completable.error(error)
        )
        whenever(ensRepository.reverseResolveENS(any())).thenReturn(
            Single.just(
                "didi.eth"
            )
        )
        repository.transferERC20Token(0, Transaction()).test().assertError(error)
    }

    @Test
    fun `resolve ens test`() {
        whenever(ensRepository.resolveENS(any())) doReturn Single.just("tom")
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
        whenever(ensRepository.resolveENS(any())) doReturn Single.error(error)
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
        whenever(blockchainTransactionRepository.getTransactions(any())).thenReturn(
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
        whenever(blockchainTransactionRepository.getTransactions(any())).thenReturn(
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
            blockchainTransactionRepository.getTransactionCosts(any(), eq(null))
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
        val transactionSpeedStats = TransactionSpeedStats(BigDecimal.TEN)

        whenever(cryptoApi.getGasPrice(any(), any())).thenReturn(
            Single.just(
                GasPrices(transactionSpeedStats, transactionSpeedStats, transactionSpeedStats, transactionSpeedStats)
            )
        )
        whenever(
            blockchainTransactionRepository.getTransactionCosts(any(), any())
        ).doReturn(Single.just(TransactionCostPayload(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN)))
        whenever(unitConverter.toGwei(BigDecimal.TEN)).thenReturn(BigDecimal.valueOf(10000000000))
        whenever(unitConverter.fromWei(BigDecimal.valueOf(10000000000))).thenReturn(BigDecimal.TEN)
        repository.getTransactionCosts(TxCostPayload(TransferType.COIN_TRANSFER, chainId = 1))
            .test()
            .assertComplete()
            .assertValue {
                it.gasPrice == BigDecimal.TEN
            }
    }

    @Test
    fun `get transaction costs success when there is gas price from oracle available and it is Matic network`() {
        NetworkManager.initialize(
            listOf(
                Network(
                    chainId = 137,
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri",
                    gasPriceOracle = "url"
                )
            )
        )
        whenever(cryptoApi.getGasPriceForMatic(any())).thenReturn(
            Single.just(
                GasPrices(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)
            )
        )
        whenever(
            blockchainTransactionRepository.getTransactionCosts(any(), any())
        ).doReturn(Single.just(TransactionCostPayload(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN)))
        whenever(unitConverter.toGwei(BigDecimal.TEN)).thenReturn(BigDecimal.valueOf(10000000000))
        whenever(unitConverter.fromWei(BigDecimal.valueOf(10000000000))).thenReturn(BigDecimal.TEN)
        repository.getTransactionCosts(TxCostPayload(TransferType.COIN_TRANSFER, chainId = 137))
            .test()
            .assertComplete()
            .assertValue {
                it.gasPrice == BigDecimal.TEN
            }
    }


    @Test
    fun `get transaction costs success when there is gas price from node available`() {
        NetworkManager.initialize(
            listOf(
                Network(
                    chainId = 56,
                    httpRpc = "httpRpc",
                    wsRpc = "wssuri",
                    gasPriceOracle = "url"
                )
            )
        )

        val weiAmount = 10000000000
        val result = "0x" + BigDecimal.valueOf(weiAmount).toBigInteger().toString(16)

        whenever(cryptoApi.getGasPriceFromRpcOverHttp(any(), any(), any())).thenReturn(
            Single.just(GasPricesFromRpcOverHttp("json", 1, result))
        )

        whenever(
            blockchainTransactionRepository.getTransactionCosts(any(), any())
        ).doReturn(Single.just(TransactionCostPayload(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN)))

        whenever(unitConverter.fromWei(BigDecimal.valueOf(10000000000))).thenReturn(BigDecimal.TEN)
        whenever(unitConverter.fromWei(BigDecimal.ZERO)).thenReturn(BigDecimal.ZERO)

        repository.getTransactionCosts(TxCostPayload(TransferType.COIN_TRANSFER, chainId = 56))
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
        whenever(blockchainTransactionRepository.getTransactionCosts(any(), eq(null))).doReturn(Single.error(error))
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
        whenever(blockchainTransactionRepository.getTransactionCosts(any(), eq(null)))
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
        whenever(validationRepository.isAddressValid(any(), anyOrNull())).thenReturn(true)
        val result = repository.isAddressValid("0x12345")
        assertEquals(true, result)
    }

    @Test
    fun `is address valid false`() {
        whenever(validationRepository.isAddressValid(any(), anyOrNull())).thenReturn(false)
        val result = repository.isAddressValid("123455")
        assertEquals(false, result)
    }

    @Test
    fun `is recipient checksum success`() {
        whenever(validationRepository.toRecipientChecksum(any(), anyOrNull())).thenReturn("checksum")
        val result = repository.toRecipientChecksum("0x12345")
        assertEquals("checksum", result)
    }

    @Test
    fun `is recipient checksum fail`() {
        whenever(validationRepository.toRecipientChecksum(any(), anyOrNull())).thenReturn("checksum")
        val result = repository.toRecipientChecksum("123455")
        assertNotEquals("otherChecksum", result)
    }

    @Test
    fun `Checking token icon updates`() {
        whenever(tokenManager.checkMissingTokensDetails()).thenReturn(Completable.complete())
        repository.checkMissingTokensDetails()
        verify(tokenManager, times(1)).checkMissingTokensDetails()
    }

    @Test
    fun `get account by address and chain id test`() {
        whenever(walletConfigManager.getWalletConfig()).thenReturn(
            WalletConfig(
                version = 1,
                accounts = listOf(Account(1, chainId = 1, address = "address"))
            )
        )
        whenever(validationRepository.toChecksumAddress(any(), isNull())).doReturn("address")
        val result = repository.getAccountByAddressAndChainId("address", 1)
        assertEquals(result?.address, "address")
        assertEquals(result?.chainId, 1)
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
        whenever(unitConverter.toEther(any())).thenReturn(BigDecimal.TEN)
        val result = repository.toUserReadableFormat(BigDecimal.TEN)
        assertEquals(result, BigDecimal.TEN)
    }

    @Test
    fun `send transaction success`() {
        whenever(
            blockchainTransactionRepository.sendWalletConnectTransaction(
                any(),
                any()
            )
        ).thenReturn(Single.just("txHash"))
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
        whenever(blockchainTransactionRepository.sendWalletConnectTransaction(any(), any())).thenReturn(
            Single.error(
                error
            )
        )
        repository.sendTransaction(111, Transaction(address = "address"))
            .test()
            .assertError(error)
    }

    @Test
    fun `Checking refreshing token balances success`() {
        val accounts = listOf(
            Account(1, "publicKey", "privateKey", "address", chainId = ChainId.ETH_RIN, _isTestNetwork = true)
        )
        val accountToken =
            AccountToken(
                ERCToken(ChainId.ETH_RIN, "one", address = "0x01", decimals = "10", type = TokenType.ERC20),
                currentRawBalance = BigDecimal.TEN
            )

        whenever(walletConfigManager.getWalletConfig()).thenReturn(WalletConfig(1, emptyList(), accounts))
        whenever(tokenManager.getTokenBalance(any())).thenReturn(
            Flowable.just(AssetBalance(ChainId.ETH_RIN, "privateKey", accountToken))
        )
        whenever(tokenManager.getTokensRates(any())).thenReturn(Completable.complete())
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())

        repository.getTokenBalance()
            .test()
            .await()
            .assertValue { asset ->
                asset is AssetBalance &&
                        asset.chainId == ChainId.ETH_RIN &&
                        asset.accountToken.token.name == "one"
            }
    }

    @Test
    fun `Checking refreshing token balances when new tokens are detected without account address`() {
        val accountToken =
            AccountToken(
                ERCToken(ChainId.ETH_RIN, "one", address = "0x01", decimals = "10", accountAddress = "", type = TokenType.ERC20),
                currentRawBalance = BigDecimal.TEN
            )
        whenever(tokenManager.getTokenBalance(any())).thenReturn(
            Flowable.just(AssetBalance(ChainId.ETH_RIN, "privateKey", accountToken))
        )
        whenever(tokenManager.getTokensRates(any())).thenReturn(Completable.complete())
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())

        repository.getTokenBalance()
            .test()
            .await()
            .assertComplete()
            .assertValueCount(7)
    }

    @Test
    fun `Checking refreshing token balances error`() {
        val error = Throwable("Balance error")
        whenever(tokenManager.getTokenBalance(any())).thenReturn(
            Flowable.just(
                AssetError(
                    1,
                    "key",
                    "accAdd",
                    "tokenAdd",
                    error,
                    null
                )
            )
        )

        val testObserver = repository.getTokenBalance().test()
        testObserver.awaitTerminalEvent()
        testObserver
            .assertNoErrors()
            .assertValueAt(
                0, AssetError(
                    1,
                    "key",
                    "accAdd",
                    "tokenAdd",
                    error,
                    null
                )
            )
    }


    @Test
    fun `Check refreshing tokens list success`() {
        val tokensList = listOf(
            ERCToken(3, "Token01", address = "0x0N3", type = TokenType.ERC20),
            ERCToken(3, "Token02", address = "0xTW0", type = TokenType.ERC20),
            ERCToken(3, "Token03", address = "0xTHR33", type = TokenType.ERC20)
        )

        val tokensMap = mapOf(Pair(3, tokensList))
        val updatedTokensMap = UpdateTokensResult(true, tokensMap)

        whenever(tokenManager.downloadTokensList(any())).thenReturn(Single.just(tokensList))
        whenever(tokenManager.sortTokensByChainId(any())).thenReturn(tokensMap)
        whenever(tokenManager.mergeWithLocalTokensList(any())).thenReturn(updatedTokensMap)
        whenever(tokenManager.updateTokenIcons(any(), any())).thenReturn(
            Single.just(updatedTokensMap),
            Single.error(Throwable("Stop thread"))
        )
        whenever(tokenManager.updateMissingNFTTokensDetails(any(), any())).thenReturn(
            Single.just(updatedTokensMap),
            Single.error(Throwable("Stop thread"))
        )
        whenever(tokenManager.mergeNFTDetailsWithRemoteConfig(any(), any())).thenReturn(
            Single.just(updatedTokensMap),
            Single.error(Throwable("Stop thread"))
        )
        whenever(tokenManager.saveTokens(any(), any())).thenReturn(
            Single.just(true),
            Single.error(Throwable("Stop thread"))
        )

        repository.discoverNewTokens().test().assertComplete().assertValue { it }
        repository.discoverNewTokens().test().assertComplete().assertValue { !it }
    }

    @Test
    fun `Check refreshing tokens list fail`() {
        val error = Throwable("error")
        val tokensMap = mapOf(Pair(3, listOf<ERCToken>()))
        val updatedTokensMap = UpdateTokensResult(true, tokensMap)
        whenever(tokenManager.downloadTokensList(any())).thenReturn(Single.error(error))
        whenever(tokenManager.sortTokensByChainId(any())).thenReturn(tokensMap)
        whenever(tokenManager.mergeWithLocalTokensList(any())).thenReturn(updatedTokensMap)
        whenever(tokenManager.updateTokenIcons(any(), any())).thenReturn(Single.just(updatedTokensMap))
        whenever(tokenManager.saveTokens(any(), any())).thenReturn(
            Single.just(true),
            Single.error(Throwable("Stop thread"))
        )

        repository.discoverNewTokens().test().assertComplete().assertNoErrors()
    }

    @Test
    fun `Check getting current tokens rate`() {
        val error = Throwable("Error-303")
        whenever(tokenManager.getTokensRates(any())).thenReturn(Completable.complete(), Completable.error(error))

        repository.getTokensRates().test().assertComplete()
        repository.getTokensRates().test().assertError(error)
        verify(tokenManager, times(2)).getTokensRates(any())
    }

    @Test
    fun `fill missing account address test`() {
        repository.newTaggedTokens = mutableListOf(
            ERCToken(ChainId.ETH_RIN, tag = "tag1", accountAddress = "address1", address = "token1", type = TokenType.ERC20),
            ERCToken(ChainId.ETH_RIN, tag = "tag1", accountAddress = "address2", address = "token2", type = TokenType.ERC20),
            ERCToken(88, tag = "tag1", accountAddress = "address2", address = "token2", type = TokenType.ERC20)
        )

        val result = repository.getTokensWithAccountAddress(walletConfig.erc20Tokens)
        result.size shouldBeEqualTo 4
        result[4]!!.size shouldBeEqualTo 4

        result[4]!![2].accountAddress shouldBeEqualTo "address1"
        result[4]!![2].tag shouldBeEqualTo "tag1"

        result[4]!![3].accountAddress shouldBeEqualTo "address2"
        result[4]!![3].tag shouldBeEqualTo "tag1"
    }

    @Test
    fun `update tokens with tagged tokens test`() {
        repository.newTaggedTokens = mutableListOf(
            ERCToken(ChainId.ETH_RIN, tag = "tag1", accountAddress = "address1", type = TokenType.ERC20),
            ERCToken(ChainId.ETH_RIN, tag = "tag2", accountAddress = "address2", type = TokenType.ERC20)
        )
        whenever(walletConfigManager.getWalletConfig()).thenReturn(MockDataProvider.walletConfig)
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        repository.updateTaggedTokens()
            .test()
            .assertComplete()
            .assertNoErrors()
        assertEquals(repository.newTaggedTokens.isEmpty(), true)
    }

    @Test
    fun `do not update tokens with tagged tokens test`() {
        repository.newTaggedTokens = mutableListOf()
        repository.updateTaggedTokens()
            .test()
            .assertComplete()
            .assertNoErrors()
        assertEquals(repository.newTaggedTokens.isEmpty(), true)
    }

    @Test
    fun `get super token init balance success test`() {
        val accounts = listOf(
            Account(1, "publicKey", "privateKey", "address", chainId = ChainId.ETH_RIN, _isTestNetwork = true)
        )
        val accountToken =
            AccountToken(
                ERCToken(
                    ChainId.ETH_RIN,
                    "one",
                    address = "address",
                    decimals = "10",
                    accountAddress = "address",
                    isStreamActive = true,
                    type = TokenType.ERC20
                ),
                currentRawBalance = BigDecimal.TEN
            )

        whenever(walletConfigManager.getWalletConfig()).thenReturn(WalletConfig(1, emptyList(), accounts))
        whenever(tokenManager.getSuperTokenBalance(any())).thenReturn(
            Flowable.just(AssetBalance(ChainId.ETH_RIN, "privateKey", accountToken))
        )
        whenever(tokenManager.getTokensRates(any())).thenReturn(Completable.complete())
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(tokenManager.activeSuperTokenStreams).thenReturn(
            mutableListOf(
                ActiveSuperToken(
                    "address",
                    "address",
                    ChainId.ETH_RIN
                )
            )
        )

        repository.getSuperTokenStreamInitBalance()
            .test()
            .await()
            .assertNoErrors()
            .assertValue {
                it is AssetBalance
            }
    }

    @Test
    fun `get super token init balance error test`() {
        val error = Throwable("Error")
        val accounts = listOf(
            Account(1, "publicKey", "privateKey", "address", chainId = ChainId.ETH_RIN, _isTestNetwork = true)
        )

        whenever(walletConfigManager.getWalletConfig()).thenReturn(WalletConfig(1, emptyList(), accounts))
        whenever(tokenManager.getSuperTokenBalance(any())).thenReturn(
            Flowable.just(
                AssetError(
                    ChainId.ETH_RIN,
                    "privateKey",
                    error = error,
                    accountAddress = "address",
                    tokenAddress = "address",
                    tokenId = null
                )
            )
        )
        whenever(tokenManager.getTokensRates(any())).thenReturn(Completable.complete())
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(tokenManager.activeSuperTokenStreams).thenReturn(
            mutableListOf(
                ActiveSuperToken(
                    "address",
                    "address",
                    ChainId.ETH_RIN
                )
            )
        )

        repository.getSuperTokenStreamInitBalance()
            .test()
            .await()
            .assertNoErrors()
            .assertValue {
                it is AssetError
            }
    }

    @Test
    fun `start super token stream test`() {
        val accounts = listOf(
            Account(1, "publicKey", "privateKey", "address", chainId = ChainId.ETH_RIN, _isTestNetwork = true)
        )
        val accountToken =
            AccountToken(
                ERCToken(
                    ChainId.ETH_RIN,
                    "one",
                    address = "address",
                    decimals = "10",
                    accountAddress = "address",
                    isStreamActive = true,
                    type = TokenType.ERC20
                ),
                currentRawBalance = BigDecimal.TEN
            )

        whenever(walletConfigManager.getWalletConfig()).thenReturn(WalletConfig(1, emptyList(), accounts))
        whenever(tokenManager.getSuperTokenBalance(any())).thenReturn(
            Flowable.just(AssetBalance(ChainId.ETH_RIN, "privateKey", accountToken))
        )
        whenever(webSocketRepositoryImpl.subscribeToBlockCreation(any())).thenReturn(Flowable.just(Unit))
        whenever(tokenManager.getTokensRates(any())).thenReturn(Completable.complete())
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(tokenManager.activeSuperTokenStreams).thenReturn(
            mutableListOf(
                ActiveSuperToken(
                    "address",
                    "address",
                    ChainId.ETH_RIN
                )
            )
        )

        repository.startSuperTokenStreaming(ChainId.ETH_RIN)
            .test()
            .await()
            .assertNoErrors()
            .assertValue {
                it is AssetBalance
            }

    }

    @Test
    fun `start super token stream error test`() {
        val error = Throwable("Error")
        val accounts = listOf(
            Account(1, "publicKey", "privateKey", "address", chainId = ChainId.ETH_RIN, _isTestNetwork = true)
        )
        val accountToken =
            AccountToken(
                ERCToken(
                    ChainId.ETH_RIN,
                    "one",
                    address = "address",
                    decimals = "10",
                    accountAddress = "address",
                    isStreamActive = true,
                    type = TokenType.ERC20
                ),
                currentRawBalance = BigDecimal.TEN
            )

        whenever(walletConfigManager.getWalletConfig()).thenReturn(WalletConfig(1, emptyList(), accounts))
        whenever(tokenManager.getSuperTokenBalance(any())).thenReturn(
            Flowable.just(
                AssetError(
                    ChainId.ETH_RIN,
                    "privateKey",
                    error = error,
                    accountAddress = "address",
                    tokenAddress = "address",
                    tokenId = null
                )
            )
        )
        whenever(webSocketRepositoryImpl.subscribeToBlockCreation(any())).thenReturn(Flowable.just(Unit))
        whenever(tokenManager.getTokensRates(any())).thenReturn(Completable.complete())
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())
        whenever(tokenManager.activeSuperTokenStreams).thenReturn(
            mutableListOf(
                ActiveSuperToken(
                    "address",
                    "address",
                    ChainId.ETH_RIN
                )
            )
        )

        repository.startSuperTokenStreaming(ChainId.ETH_RIN)
            .test()
            .await()
            .assertNoErrors()
            .assertValue {
                it is AssetError
            }

    }
}