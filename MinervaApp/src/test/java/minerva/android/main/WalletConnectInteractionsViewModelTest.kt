package minerva.android.main

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.walletconnect.*
import minerva.android.extension.empty
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.Event
import minerva.android.main.walletconnect.WalletConnectInteractionsViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.defs.TxType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxSpeed
import minerva.android.walletmanager.model.walletconnect.*
import minerva.android.walletmanager.provider.UnsupportedNetworkRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.*
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.any
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class WalletConnectInteractionsViewModelTest : BaseViewModelTest() {

    private val walletConnectRepository: WalletConnectRepository = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val tokenManager: TokenManager = mock()
    private val logger: Logger = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val accountManager: AccountManager = mock()
    private val unsupportedNetworkRepository: UnsupportedNetworkRepository =mock()

    private val viewModel: WalletConnectInteractionsViewModel =
        WalletConnectInteractionsViewModel(
            transactionRepository,
            walletConnectRepository,
            logger,
            tokenManager,
            accountManager,
            walletActionsRepository,
            unsupportedNetworkRepository
        )

    private val requestObserver: Observer<WalletConnectState> = mock()
    private val requestCaptor: KArgumentCaptor<WalletConnectState> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = org.amshove.kluent.mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()


    private val meta = WalletConnectPeerMeta(name = "token", url = "test.xdai.com", description = "dsc")

    @Test
    fun `reconnect to saved sessions and handle on eth sign test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSign("messsage", "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        val account = Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        viewModel.getWalletConnectSessions()
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSignRequest
        }
    }

    @Test
    fun `do not reconnect when no sessions saved test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic(), 1))
        )
        whenever(walletConnectRepository.getSessions()).thenReturn(Single.just(listOf()))
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        viewModel.getWalletConnectSessions()
        verify(walletConnectRepository, times(0)).connect(any(), any(), any(), any())
    }

    @Test
    fun `reconnect to saved sessions and do not handle request test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic(), 1))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        viewModel.getWalletConnectSessions()
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is DefaultRequest
        }
    }

    @Test
    fun `reconnect to saved sessions and disconnect request occurs test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnDisconnect())
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        viewModel.getWalletConnectSessions()
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnDisconnected
        }
    }

    @Test
    fun `reconnect to saved sessions and handle on eth send transaction test`() {
        val tx = WalletConnectTransaction("from", "to", value = "0x100000000", data = "0x0")
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(tx, "peerId"))
        )
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")

        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                transaction.transactionType shouldBeEqualTo TransferType.COIN_TRANSFER
                transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                transaction.txCost.formattedCryptoCost shouldBeEqualTo "10"
                transaction.data shouldBeEqualTo "0x0"
                transaction.value shouldBeEqualTo "10"
                transaction.from shouldBeEqualTo "from"
            }
        }
    }

    @Test
    fun `token approve transaction test`() {
        val transition = WalletConnectTransaction(
            "from",
            "to",
            value = null,
            data = "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            tokenTransaction = TokenTransaction(tokenSymbol = "WTF")
        )
        val account =
            Account(
                1,
                cryptoBalance = BigDecimal.TEN,
                fiatBalance = BigDecimal(13),
                chainId = ETH_MAIN,
                accountTokens = mutableListOf(
                    AccountToken(
                        ERCToken(1, symbol = "WTF", type = TokenType.ERC20)
                    )
                )
            )
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")
        whenever(transactionRepository.getTokenFiatRate(any())).thenReturn(Single.just(3.0))

        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.TOKEN_SWAP_APPROVAL
                this.transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                this.transaction.txCost.formattedCryptoCost shouldBeEqualTo "10"
                this.transaction.data shouldBeEqualTo "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                this.transaction.value shouldBeEqualTo "0"
                this.transaction.from shouldBeEqualTo "from"
            }
        }
    }

    @Test
    fun `token swap transaction test`() {
        val transition = WalletConnectTransaction(
            "from",
            "to",
            value = null,
            data = "0x38ed1739000000000000000000000000000000000000000000000000000002ba7def30000000000000000000000000000000000000000000000000000010fc898105daf400000000000000000000000000000000000000000000000000000000000000a000000000000000000000000072f4d6cb761fb9bab743f35f60eb463f3291b4a10000000000000000000000000000000000000000000000000000000060449fa00000000000000000000000000000000000000000000000000000000000000004000000000000000000000000f1738912ae7439475712520797583ac784ea90330000000000000000000000006a023ccd1ff6f2045c3309768ead9e68f978f6e1000000000000000000000000e91d153e0b41518a2ce8dd3d7944fa863463a97d0000000000000000000000008a95ea379e1fa4c749dd0a7a21377162028c479e"
        )
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(transactionRepository.getTokenFiatRate(any())).thenReturn(Single.just(3.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")

        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.TOKEN_SWAP
                this.transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                this.transaction.txCost.formattedCryptoCost shouldBeEqualTo "10"
                this.transaction.value shouldBeEqualTo "0"
                this.transaction.from shouldBeEqualTo "from"
            }
        }
    }

    @Test
    fun `swap extra tokens for tokens transaction test`() {
        val transition = WalletConnectTransaction(
            "from",
            "to",
            value = null,
            data = "0x38ed1739000000000000000000000000000000000000000000000000000002ba7def30000000000000000000000000000000000000000000000000000010fc898105daf400000000000000000000000000000000000000000000000000000000000000a000000000000000000000000072f4d6cb761fb9bab743f35f60eb463f3291b4a10000000000000000000000000000000000000000000000000000000060449fa00000000000000000000000000000000000000000000000000000000000000004000000000000000000000000f1738912ae7439475712520797583ac784ea90330000000000000000000000006a023ccd1ff6f2045c3309768ead9e68f978f6e1000000000000000000000000e91d153e0b41518a2ce8dd3d7944fa863463a97d0000000000000000000000008a95ea379e1fa4c749dd0a7a21377162028c479e"
        )
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")
        whenever(transactionRepository.getTokenFiatRate(any())).thenReturn(Single.just(3.0))

        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.TOKEN_SWAP
                this.transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                this.transaction.txCost.formattedCryptoCost shouldBeEqualTo "10"
                this.transaction.value shouldBeEqualTo "0"
                this.transaction.from shouldBeEqualTo "from"
            }
        }
    }

    @Test
    fun `token swap transaction with hex data transfer method test`() {
        val transition = WalletConnectTransaction(
            "from",
            "to",
            value = null,
            data = "0xa9059cbb000000000000000000000000e602118e3658a433b60e6f7ced1186fde6df6f5d000000000000000000000000000000000000000000000000000009184e72a000"
        )
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")
        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                transaction.transactionType shouldBeEqualTo TransferType.DEFAULT_TOKEN_TX
            }
        }
    }

    @Test
    fun `parse undefined data contract transaction test`() {
        val transition = WalletConnectTransaction(
            "from",
            "to",
            value = null,
            data = "0xa93333602118e3658a433b60e6f7ced1186fde6df6f5d000000000000000000000000000000000000000000000000000009184e72a000"
        )
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")
        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                transaction.transactionType shouldBeEqualTo TransferType.DEFAULT_TOKEN_TX
            }
        }
    }

    @Test
    fun `parse error data contract transaction test`() {
        val transition = WalletConnectTransaction("from", "to", value = null, data = "203DASCS3UE   DBDJHF DFSDFD")
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")
        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                transaction.transactionType shouldBeEqualTo TransferType.COIN_TRANSFER
            }
        }
    }

    @Test
    fun `coin swap transaction test`() {
        val transition = WalletConnectTransaction(
            "from",
            "to",
            value = "0x1234",
            data = "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        )
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.COIN_SWAP
                this.transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                this.transaction.txCost.formattedCryptoCost shouldBeEqualTo "10"
                this.transaction.data shouldBeEqualTo "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                this.transaction.value shouldBeEqualTo "10"
                this.transaction.from shouldBeEqualTo "from"
            }
        }
    }

    @Test
    fun `send transaction test success`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic(), 1))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.sendTransaction(any(), any())).thenReturn(Single.just("txHash"))
        doNothing().whenever(walletConnectRepository).approveTransactionRequest(any(), any())
        viewModel.getWalletConnectSessions()
        viewModel.currentDappSession = DappSession(address = "address1", peerId = "id")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is ProgressBarState
        }
    }

    @Test
    fun `send transaction test error`() {
        val error = Throwable()
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic(), 1))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).rejectRequest(any())
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.sendTransaction(any(), any())).thenReturn(Single.error(error))
        viewModel.getWalletConnectSessions()
        viewModel.currentDappSession = DappSession(address = "address1", peerId = "id")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnGeneralError
        }
    }

    @Test
    fun `recalculate tx cost`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic(), 1))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")
        viewModel.getWalletConnectSessions()
        val result = viewModel.recalculateTxCost(
            BigDecimal.TEN,
            WalletConnectTransaction(txCost = TransactionCost(gasLimit = BigInteger.ONE))
        )
        assertEquals(result.txCost.cost, BigDecimal.TEN)
    }

    @Test
    fun `is balance to low`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic(), 1))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.TEN)
        viewModel.getWalletConnectSessions()
        val result = viewModel.isBalanceTooLow(BigDecimal.TEN, BigDecimal.ONE)
        assertEquals(result, false)

        val result2 = viewModel.isBalanceTooLow(BigDecimal.ONE, BigDecimal.TEN)
        assertEquals(result2, true)
    }

    @Test
    fun `approve request test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic(), 1))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(Account(1))
        viewModel.getWalletConnectSessions()
        viewModel.currentDappSession = DappSession(address = "address1", peerId = "id")
        viewModel.acceptRequest(isMobileWalletConnect = false)
        verify(walletConnectRepository).approveRequest(any(), any())
    }

    @Test
    fun `reject request test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic(), 1))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.TEN)
        viewModel.getWalletConnectSessions()
        viewModel.currentDappSession = DappSession(address = "address1", peerId = "id")
        viewModel.rejectRequest(isMobileWalletConnect = false)
        verify(walletConnectRepository).rejectRequest(any())
    }

    @Test
    fun `coin swap transaction test when transaction value is invalid`() {
        val transition = WalletConnectTransaction(
            "from",
            "to",
            value = "111",
            data = "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        )
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any(), any())
        whenever(transactionRepository.getAccountByAddressAndChainId(any(), any())).thenReturn(account)
        whenever(
            transactionRepository.toUserReadableFormat
                (any())
        ).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getCoinFiatRate(any())).thenReturn(Single.just(2.0))
        whenever(transactionRepository.getFiatSymbol()).thenReturn("EUR")
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))

        viewModel.getWalletConnectSessions()
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is WrongTransactionValueState
            (firstValue as WrongTransactionValueState).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.UNKNOWN
                this.transaction.data shouldBeEqualTo "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                this.transaction.value shouldBeEqualTo "111"
                this.transaction.from shouldBeEqualTo "from"
            }
        }
    }

    @Test
    fun `on disconnect event test`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnDisconnect()))
        viewModel.walletConnectStatus.observeForever(requestObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnDisconnected
        }
    }

    @Test
    fun `on connection failure event test`() {
        val error = Throwable("timeout")
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }


    @Test
    fun `on session request event test with defined chainId and no available accounts`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        whenever(accountManager.getFirstActiveAccountOrNull(1)).thenReturn(null)
        viewModel.account = Account(1, chainId = 1)
        viewModel.walletConnectStatus.observeForever(requestObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue shouldBeEqualTo minerva.android.accounts.walletconnect.OnSessionRequest(
                meta,
                BaseNetworkData(Int.InvalidId, String.Empty),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `on session request event test with defined chainId and available account`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        whenever(accountManager.getFirstActiveAccountOrNull(1)).thenReturn(
            Account(1, chainId = 1, address = "address1", _isTestNetwork = true, isHide = false)
        )
        viewModel.account = Account(1, chainId = 1)
        viewModel.walletConnectStatus.observeForever(requestObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue shouldBeEqualTo minerva.android.accounts.walletconnect.OnSessionRequest(
                meta,
                BaseNetworkData(Int.InvalidId, String.Empty),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `on session request event test with not defined chainId`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, null, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        whenever(accountManager.getFirstActiveAccountOrNull(1)).thenReturn(null)
        viewModel.account = Account(1, chainId = 1)
        viewModel.walletConnectStatus.observeForever(requestObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue shouldBeEqualTo minerva.android.accounts.walletconnect.OnSessionRequest(
                meta,
                BaseNetworkData(Int.InvalidId, String.empty),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `on session request event test with unsupported chainId`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 134, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        whenever(unsupportedNetworkRepository.getNetworkName(134)).thenReturn(Single.just("networkname"))
        whenever(accountManager.getFirstActiveAccountOrNull(1)).thenReturn(null)
        viewModel.account = Account(1, chainId = 1)
        viewModel.walletConnectStatus.observeForever(requestObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        requestCaptor.run {
            verify(requestObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo minerva.android.accounts.walletconnect.OnSessionRequest(
                meta,
                BaseNetworkData(134, String.empty),
                WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
            )
            secondValue shouldBeEqualTo UpdateOnSessionRequest(
                BaseNetworkData(134, "networkname"),
                WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `check creating new account flow`() {
        NetworkManager.initialize(listOf(Network(chainId = 3, name = "xDai", httpRpc = "some_rpc")))
        whenever(accountManager.createOrUnhideAccount(any())).thenReturn(Single.just("Cookie Account"))
        whenever(walletActionsRepository.saveWalletActions(com.nhaarman.mockitokotlin2.any())).thenReturn(Completable.complete())
        viewModel.run {
            walletConnectStatus.observeForever(requestObserver)
            errorLiveData.observeForever(errorObserver)
            requestedNetwork = BaseNetworkData(3, "xDai")
            addAccount(3, WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR)
            addAccount(3, WalletConnectAlertType.UNDEFINED_NETWORK_WARNING)
        }

        requestCaptor.run {
            verify(requestObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo UpdateOnSessionRequest(BaseNetworkData(3, "xDai"), WalletConnectAlertType.NO_ALERT)
            secondValue shouldBeEqualTo UpdateOnSessionRequest(
                BaseNetworkData(3, "xDai"),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `handle wrong wc qr code test`() {
        viewModel.walletConnectStatus.observeForever(requestObserver)
        viewModel.handleDeepLink("wc:123456789")
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue shouldBe WrongWalletConnectCodeState
        }
    }

    @Test
    fun `handle wc qr code test`() {
        whenever(walletConnectRepository.getWCSessionFromQr(any())).thenReturn(
            WalletConnectSession(
                topic = "topic",
                version = "v",
                bridge = "b",
                key = "k"
            )
        )
        viewModel.walletConnectStatus.observeForever(requestObserver)
        viewModel.handleDeepLink("wc:123456789bridge=123key=123")
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue shouldBe CorrectWalletConnectCodeState
        }
    }

    @Test
    fun `approve session test and close app state `() {
        NetworkManager.initialize(listOf(Network(name = "Ethereum", chainId = 2, testNet = false, httpRpc = "url")))
        viewModel.topic = Topic()
        viewModel.currentSession = WalletConnectSession("topic", "version", "bridge", "key")
        viewModel.account = Account(1, chainId = 2)
        viewModel.walletConnectStatus.observeForever(requestObserver)
        whenever(
            walletConnectRepository.approveSession(
                any(), any(), any(), any()
            )
        ).thenReturn(Completable.complete())
        viewModel.approveSession(WalletConnectPeerMeta(name = "name", url = "url"), isMobileWalletConnect = true)
        verify(walletConnectRepository).approveSession(
            any(), any(), any(), any()
        )
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is CloseScannerState
        }
    }

    @Test
    fun `approve session test and close dialog state `() {
        NetworkManager.initialize(listOf(Network(name = "Ethereum", chainId = 2, testNet = false, httpRpc = "url")))
        viewModel.topic = Topic()
        viewModel.currentSession = WalletConnectSession("topic", "version", "bridge", "key")
        viewModel.account = Account(1, chainId = 2)
        viewModel.walletConnectStatus.observeForever(requestObserver)
        whenever(
            walletConnectRepository.approveSession(
                any(), any(), any(), any()
            )
        ).thenReturn(Completable.complete())
        viewModel.approveSession(WalletConnectPeerMeta(name = "name", url = "url"), isMobileWalletConnect = false)
        verify(walletConnectRepository).approveSession(
            any(), any(), any(), any()
        )
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is CloseDialogState
        }
    }


    @Test
    fun `reject session test`() {
        viewModel.topic = Topic()
        viewModel.rejectSession()
        verify(walletConnectRepository).rejectSession("")
    }
}