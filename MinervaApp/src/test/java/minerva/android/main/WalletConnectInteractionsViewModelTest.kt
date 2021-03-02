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
        doNothing().whenever(walletConnectRepository).connect(any(), any(), any())
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
        val transition = WalletConnectTransaction("from", "to", value = "100000000", data = "0x0")
        val account =
            Account(1, cryptoBalance = BigDecimal.TEN, fiatBalance = BigDecimal(13), networkShort = "eth_mainnet")
        NetworkManager.initialize(listOf(Network(short = "eth_mainnet", httpRpc = "url")))
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

        viewModel = WalletConnectInteractionsViewModel(transactionRepository, walletConnectRepository)
        viewModel.currentAccount = account
        viewModel.currentDappSession = DappSession(address = "address1")
        viewModel.walletConnectStatus.observeForever(requestObserver)
        requestCaptor.run {
            verify(requestObserver).onChanged(capture())
            firstValue is OnEthSendTransactionRequest
            transition.txCost.cost shouldBeEqualTo BigDecimal.ZERO
            transition.txCost.formattedCryptoCost shouldBeEqualTo "0.000000"
            transition.data shouldBeEqualTo "0x0"
            transition.value shouldBeEqualTo "10"
            transition.from shouldBeEqualTo "from"
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