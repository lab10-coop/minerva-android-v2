package minerva.android.main

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.walletconnect.DefaultRequest
import minerva.android.accounts.walletconnect.OnDisconnected
import minerva.android.accounts.walletconnect.OnEthSignRequest
import minerva.android.accounts.walletconnect.WalletConnectState
import minerva.android.main.walletconnect.*
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.Topic
import minerva.android.walletmanager.model.WalletConnectPeerMeta
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnEthSign
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import org.amshove.kluent.any
import org.junit.Test

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
        whenever(walletConnectRepository.getSessions()).thenReturn(Single.just(listOf()))
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
}