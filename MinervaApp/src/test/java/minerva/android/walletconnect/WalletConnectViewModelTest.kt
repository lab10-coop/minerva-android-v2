package minerva.android.walletconnect

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import minerva.android.BaseViewModelTest
import minerva.android.accounts.walletconnect.*
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.Topic
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WalletConnectViewModelTest : BaseViewModelTest() {

    private val repository: WalletConnectRepository = mock()
    private val manager: AccountManager = mock()
    private lateinit var viewModel: WalletConnectViewModel
    private val stateObserver: Observer<WalletConnectState> = mock()
    private val stateCaptor: KArgumentCaptor<WalletConnectState> = argumentCaptor()
    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()
    private val meta = WalletConnectPeerMeta(name = "token", url = "test.xdai.com", description = "dsc")

    @Before
    fun setup() {
        viewModel = WalletConnectViewModel(manager, repository)
    }

    @Test
    fun `on connection failure event test`() {
        val error = Throwable("timeout")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.setConnectionStatusFlowable()
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `on connection error event test`() {
        val error = Throwable("timeout")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.setConnectionStatusFlowable()
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `on disconnect event test`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnDisconnect()))
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState &&
                    secondValue is OnDisconnected
        }
    }


    @Test
    fun `on session request event test with defined chainId on test net`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token="Ethereum"))
        viewModel.account = Account(1, chainId = 1)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(meta, "Ethereum", WalletConnectAlertType.NO_ALERT)
        }
    }

    @Test
    fun `on session request event test with defined chainId on test net with xDai account, Ethereum dapp and url that contain xDai`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "xDai", chainId = 2, token="xDai"), Network(name = "Ethereum", chainId = 1, token="Ethereum"))
        viewModel.account = Account(1, chainId = 2)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(meta, "Ethereum", WalletConnectAlertType.NO_ALERT)
        }
    }

    @Test
    fun `on session request event test with defined chainId on test net with xDai account, Ethereum dapp and url that doesnt contain xDai`() {
        val meta = WalletConnectPeerMeta(name = "token", url = "test.com", description = "dsc")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "xDai", chainId = 2, token="xDai"), Network(name = "Ethereum", chainId = 1, token="Ethereum"))
        viewModel.account = Account(1, chainId = 2)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(meta, "Ethereum", WalletConnectAlertType.WARNING)
        }
    }

    @Test
    fun `on session request event test with defined chainId on test net with Ethereum account and xdai dapp`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 2, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "xDai", chainId = 2, token="xDai"), Network(name = "Ethereum", chainId = 1, token="Ethereum"))
        viewModel.account = Account(1, chainId = 1)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(meta, "xDai", WalletConnectAlertType.ERROR)
        }
    }

    @Test
    fun `on session request event test with not defined chainId on test net`() {
        val meta = WalletConnectPeerMeta(name = "token", url = "url", description = "dsc")
        val networks =
            listOf(
                Network(
                    name = "Ethereum (Görli)",
                    chainId = 5,
                    httpRpc = "someaddress",
                    testNet = true
                )
            )
        NetworkManager.initialize(networks)
        whenever(repository.connectionStatusFlowable).thenReturn(
            Flowable.just(
                    OnSessionRequest(meta, null, Topic("peerID", "remotePeerID"), 1)
                )
            )
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.account = Account(1, chainId = 5)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(meta, "Ethereum (Görli)", WalletConnectAlertType.UNDEFINED_NETWORK_WARNING)
        }
    }

    @Test
    fun `on session request event test with defined chainId on main net`() {
        val meta = WalletConnectPeerMeta(name = "token", url = "url", description = "dsc")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(
                Flowable.just(
                    OnSessionRequest(
                        meta,
                        null,
                        Topic("peerID", "remotePeerID"),
                        1
                    )
                )
            )
        NetworkManager.networks =
            listOf(
                Network(
                    name = "Ethereum",
                    chainId = 1,
                    testNet = false,
                    httpRpc = "url"
                )
            )
        viewModel.account = Account(1, chainId = 1)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState
            secondValue shouldBeEqualTo OnSessionRequest(meta, "Ethereum", WalletConnectAlertType.UNDEFINED_NETWORK_WARNING)
            viewModel.requestedNetwork shouldBeEqualTo "Ethereum"
        }
    }

    @Test
    fun `handle wc qr code test`() {
        whenever(repository.getWCSessionFromQr(any())).thenReturn(
            WalletConnectSession(
                topic = "topic",
                version = "v",
                bridge = "b",
                key = "k"
            )
        )
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.handleQrCode("wc:123456789")
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue shouldBe CorrectQrCodeState
        }
    }

    @Test
    fun `handle not wc qr code test`() {
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.handleQrCode("qr:123456789")
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue shouldBe WrongQrCodeState
        }
    }

    @Test
    fun `approve session test`() {
        NetworkManager.initialize(
            listOf(
                Network(
                    name = "Ethereum",
                    chainId = 2,
                    testNet = false,
                    httpRpc = "url"
                )
            )
        )
        viewModel.topic = Topic()
        viewModel.currentSession = WalletConnectSession("topic", "version", "bridge", "key")
        viewModel.account = Account(1, chainId = 2)
        viewModel.stateLiveData.observeForever(stateObserver)
        whenever(repository.approveSession(any(), any(), any(), any())).thenReturn(Completable.complete())
        viewModel.approveSession(WalletConnectPeerMeta(name = "name", url = "url"))
        verify(repository).approveSession(any(), any(), any(), any())
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue is CloseScannerState
        }
    }

    @Test
    fun `reject session test`() {
        viewModel.topic = Topic()
        viewModel.rejectSession()
        verify(repository).rejectSession("")
    }

    @Test
    fun `kill session test`() {
        whenever(repository.killSession(any())).thenReturn(Completable.complete())
        viewModel.killSession("peerID")
        viewModel.stateLiveData.observeForever(stateObserver)
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue is OnSessionDeleted
        }
    }

    @Test
    fun `get account test`() {
        whenever(manager.loadAccount(any())).thenReturn(Account(1))
        whenever(repository.getSessionsFlowable()).thenReturn(
            Flowable.just(listOf(DappSession(address = "address")))
        )
        viewModel.getAccount(1)
        assertEquals(1, viewModel.account.id)
    }

    @Test
    fun `close scanner test`() {
        viewModel.closeScanner()
        viewModel.stateLiveData.observeForever(stateObserver)
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
        }
    }
}