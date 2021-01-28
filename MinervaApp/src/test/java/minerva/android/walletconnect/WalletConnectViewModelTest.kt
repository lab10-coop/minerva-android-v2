package minerva.android.walletconnect

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import minerva.android.BaseViewModelTest
import minerva.android.accounts.walletconnect.*
import minerva.android.walletConnect.client.OnConnectionFailure
import minerva.android.walletConnect.client.OnDisconnect
import minerva.android.walletConnect.client.OnSessionRequest
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletConnect.model.session.Topic
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletConnect.repository.WalletConnectRepository
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.repository.walletconnect.DappSessionRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WalletConnectViewModelTest : BaseViewModelTest() {

    private val repository: WalletConnectRepository = mock()
    private val dappSessionRepository: DappSessionRepository = com.nhaarman.mockitokotlin2.mock()
    private val manager: AccountManager = mock()
    private lateinit var viewModel: WalletConnectViewModel
    private val viewStateObserver: Observer<WalletConnectViewState> = mock()
    private val viewStateCaptor: KArgumentCaptor<WalletConnectViewState> = argumentCaptor()
    private val meta = WCPeerMeta(name = "token", url = "url", description = "dsc")

    @Before
    fun setup() {
        viewModel = WalletConnectViewModel(dappSessionRepository, manager, repository)
    }

    @Test
    fun `on connection failure event test`() {
        val error = Throwable("timeout")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnConnectionFailure(error, "peerId")))
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewModel.setConnectionStatusFlowable()
        viewStateCaptor.run {
            verify(viewStateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState &&
                    secondValue is OnError
        }
    }

    @Test
    fun `on connection error event test`() {
        val error = Throwable("timeout")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.error(error))
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewModel.setConnectionStatusFlowable()
        viewStateCaptor.run {
            verify(viewStateObserver).onChanged(capture())
            firstValue is OnError
        }
    }

    @Test
    fun `on disconnect event test`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnDisconnect(1, "peerID")))
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewModel.setConnectionStatusFlowable()
        viewStateCaptor.run {
            verify(viewStateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState &&
                    secondValue is OnDisconnected
        }
    }


    @Test
    fun `on session request event test with defined chainId on test net`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"))))
        NetworkManager.networks = listOf(Network(full = "Ethereum", chainId = 1))
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewModel.setConnectionStatusFlowable()
        viewStateCaptor.run {
            verify(viewStateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequestWithDefinedNetwork(meta, "Ethereum")
        }
    }

    @Test
    fun `on session request event test with not defined chainId on test net`() {
        val meta = WCPeerMeta(name = "token", url = "url", description = "dsc")
        NetworkManager.networks =
            listOf(
                Network(
                    full = "Ethereum (Görli)",
                    chainId = 1,
                    short = "eth_goerli",
                    httpRpc = "someaddress",
                    testNet = true
                )
            )

        whenever(repository.connectionStatusFlowable)
            .thenReturn(
                Flowable.just(
                    OnSessionRequest(
                        meta,
                        null,
                        Topic("peerID", "remotePeerID")
                    )
                )
            )
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewModel.account = Account(1, networkShort = "eth_goerli")
        viewModel.setConnectionStatusFlowable()
        viewStateCaptor.run {
            verify(viewStateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo
                    OnSessionRequestWithUndefinedNetwork(meta, "Ethereum (Görli)")
        }
    }

    @Test
    fun `on session request event test with defined chainId on main net`() {
        val meta = WCPeerMeta(name = "token", url = "url", description = "dsc")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(
                Flowable.just(
                    OnSessionRequest(
                        meta,
                        null,
                        Topic("peerID", "remotePeerID")
                    )
                )
            )
        NetworkManager.networks =
            listOf(
                Network(
                    full = "Ethereum",
                    chainId = 1,
                    short = "eth_mainnet",
                    testNet = false,
                    httpRpc = "url"
                )
            )
        viewModel.account = Account(1, networkShort = "eth_mainnet")
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewModel.setConnectionStatusFlowable()
        viewStateCaptor.run {
            verify(viewStateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState
            secondValue shouldBeEqualTo
                    OnSessionRequestWithUndefinedNetwork(meta, "Ethereum")
            viewModel.requestedNetwork shouldBe "Ethereum"
        }
    }

    @Test
    fun `handle wc qr code test`() {
        whenever(repository.getWCSessionFromQr(any())).thenReturn(
            WCSession(
                topic = "topic",
                version = "v",
                bridge = "b",
                key = "k"
            )
        )
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewModel.handleQrCode("wc:123456789")
        viewStateCaptor.run {
            verify(viewStateObserver).onChanged(capture())
            firstValue shouldBe CorrectQrCodeState
        }
    }

    @Test
    fun `handle not wc qr code test`() {
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewModel.handleQrCode("qr:123456789")
        viewStateCaptor.run {
            verify(viewStateObserver).onChanged(capture())
            firstValue shouldBe WrongQrCodeState
        }
    }

    @Test
    fun `approve session test`() {
        NetworkManager.initialize(
            listOf(
                Network(
                    full = "Ethereum",
                    chainId = 1,
                    short = "eth_mainnet",
                    testNet = false,
                    httpRpc = "url"
                )
            )
        )
        viewModel.topic = Topic()
        viewModel.currentSession = WCSession("topic", "version", "bridge", "key")
        viewModel.account = Account(1, networkShort = "eth_mainnet")
        whenever(dappSessionRepository.saveDappSession(any())).thenReturn(Completable.complete())
        viewModel.approveSession(WCPeerMeta(name = "name", url = "url"))
        verify(repository).approveSession(any(), any(), any())
    }

    @Test
    fun `reject session test`() {
        viewModel.topic = Topic()
        viewModel.rejectSession()
        verify(repository).rejectSession("")
    }

    @Test
    fun `kill session test`() {
        whenever(dappSessionRepository.deleteDappSession(any())).thenReturn(Completable.complete())
        viewModel.killSession("peerID")
        verify(repository).killSession("peerID")
    }

    @Test
    fun `get account test`() {
        whenever(manager.loadAccount(any())).thenReturn(Account(1))
        whenever(dappSessionRepository.getSessionsFlowable()).thenReturn(
            Flowable.just(listOf(DappSession(address = "address")))
        )
        viewModel.getAccount(1)
        assertEquals(1, viewModel.account.id)
    }

    @Test
    fun `close scanner test`() {
        viewModel.closeScanner()
        viewModel.viewStateLiveData.observeForever(viewStateObserver)
        viewStateCaptor.run {
            verify(viewStateObserver).onChanged(capture())
        }
    }
}