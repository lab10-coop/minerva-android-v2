package minerva.android.main

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.walletconnect.*
import minerva.android.main.walletconnect.WalletConnectInteractionsViewModel
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.ChainId.Companion.ETH_MAIN
import minerva.android.walletmanager.model.defs.TransferType
import minerva.android.walletmanager.model.defs.TxType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxSpeed
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.Topic
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectTransaction
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.*
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class WalletConnectInteractionsViewModelTest : BaseViewModelTest() {

    private val walletConnectRepository: WalletConnectRepository = mock()
    private val transactionRepository: TransactionRepository = mock()
    private lateinit var viewModel: WalletConnectInteractionsViewModel

    private val requestObserver: Observer<WalletConnectState> = mock()
    private val requestCaptor: KArgumentCaptor<WalletConnectState> = argumentCaptor()

    @Test
    fun `reconnect to saved sessions and handle on eth sign test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSign("messsage", "peerId"))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
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
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
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
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic()))
        )
        whenever(walletConnectRepository.getSessions()).thenReturn(Single.just(listOf()))
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        verify(walletConnectRepository, times(0)).connect(any(), any(), any())
    }

    @Test
    fun `reconnect to saved sessions and do not handle request test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic()))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
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
            Flowable.just(OnDisconnect)
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnDisconnected
        }
    }

    @Test
    fun `reconnect to saved sessions and handle on eth send transaction test`() {
        val tx = WalletConnectTransaction("from", "to", value = "100000000", data = "0x0")
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
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                transaction.transactionType shouldBeEqualTo TransferType.COIN_TRANSFER
                transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                transaction.txCost.formattedCryptoCost shouldBeEqualTo "10.000000"
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
            data = "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        )
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), chainId = ETH_MAIN)
        NetworkManager.initialize(listOf(Network(chainId = ETH_MAIN, httpRpc = "url")))
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnEthSendTransaction(transition, "peerId"))
        )
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.TOKEN_SWAP_APPROVAL
                this.transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                this.transaction.txCost.formattedCryptoCost shouldBeEqualTo "10.000000"
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
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.TOKEN_SWAP
                this.transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                this.transaction.txCost.formattedCryptoCost shouldBeEqualTo "10.000000"
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
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.TOKEN_SWAP
                this.transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                this.transaction.txCost.formattedCryptoCost shouldBeEqualTo "10.000000"
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
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                transaction.transactionType shouldBeEqualTo  TransferType.DEFAULT_TOKEN_TX
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
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                transaction.transactionType shouldBeEqualTo  TransferType.DEFAULT_TOKEN_TX
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
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                transaction.transactionType shouldBeEqualTo  TransferType.COIN_TRANSFER
            }
        }
    }

    @Test
    fun `coin swap transaction test`() {
        val transition = WalletConnectTransaction(
            "from",
            "to",
            value = "22",
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
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(account)
        whenever(transactionRepository.toEther(any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getTransactionCosts(any()))
            .thenReturn(
                Single.just(
                    TransactionCost(
                        BigDecimal.TEN, BigInteger.TEN, BigDecimal.TEN, "12",
                        listOf(TxSpeed(TxType.FAST, BigDecimal(1)))
                    )
                )
            )
        whenever(transactionRepository.getEurRate(any())).thenReturn(Single.just(2.0))
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            (firstValue as OnEthSendTransactionRequest).apply {
                this.transaction.transactionType shouldBeEqualTo TransferType.COIN_SWAP
                this.transaction.txCost.cost shouldBeEqualTo BigDecimal.TEN
                this.transaction.txCost.formattedCryptoCost shouldBeEqualTo "10.000000"
                this.transaction.data shouldBeEqualTo "0x095ea7b30000000000000000000000001c232f01118cb8b424793ae03f870aa7d0ac7f77ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                this.transaction.value shouldBeEqualTo "10"
                this.transaction.from shouldBeEqualTo "from"
            }
        }
    }

    @Test
    fun `send transaction test success`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic()))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.sendTransaction(any(), any())).thenReturn(Single.just("txHash"))
        doNothing().whenever(walletConnectRepository).approveTransactionRequest(any(), any())
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
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
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic()))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).rejectRequest(any())
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(listOf(DappSession())))
        whenever(transactionRepository.sendTransaction(any(), any())).thenReturn(Single.error(error))
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentDappSession = DappSession(address = "address1", peerId = "id")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnError
        }
    }

    @Test
    fun `recalculate tx cost`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic()))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.TEN)
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)

        val result = viewModel.recalculateTxCost(
            BigDecimal.TEN,
            WalletConnectTransaction(txCost = TransactionCost(gasLimit = BigInteger.ONE))
        )
        assertEquals(result.txCost.cost, BigDecimal.TEN)
    }

    @Test
    fun `is balance to low`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic()))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.TEN)
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)

        val result = viewModel.isBalanceTooLow(BigDecimal.TEN, BigDecimal.ONE)
        assertEquals(result, false)

        val result2 = viewModel.isBalanceTooLow(BigDecimal.ONE, BigDecimal.TEN)
        assertEquals(result2, true)
    }

    @Test
    fun `approve request test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic()))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.TEN)
        whenever(transactionRepository.getAccountByAddress(any())).thenReturn(Account(1))
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentDappSession = DappSession(address = "address1", peerId = "id")
        viewModel.acceptRequest()
        verify(walletConnectRepository).approveRequest(any(), any())
    }

    @Test
    fun `reject request test`() {
        whenever(walletConnectRepository.connectionStatusFlowable).thenReturn(
            Flowable.just(OnSessionRequest(WalletConnectPeerMeta(), 1, Topic()))
        )
        whenever(walletConnectRepository.getDappSessionById(any())).thenReturn(Single.just(DappSession(address = "address1")))
        whenever(walletConnectRepository.getSessions()).thenReturn(
            Single.just(listOf(DappSession(address = "address1"), DappSession(address = "address2")))
        )
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.TEN)
        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentDappSession = DappSession(address = "address1", peerId = "id")
        viewModel.rejectRequest()
        verify(walletConnectRepository).rejectRequest(any())
    }
}