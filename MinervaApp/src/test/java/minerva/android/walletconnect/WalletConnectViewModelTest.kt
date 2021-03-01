package minerva.android.walletconnect

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import minerva.android.BaseViewModelTest
import minerva.android.accounts.walletconnect.*
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.Topic
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnFailure
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
    private val meta = WalletConnectPeerMeta(name = "token", url = "url", description = "dsc")

    @Before
    fun setup() {
        viewModel = WalletConnectViewModel(manager, repository)
    }

    @Test
    fun `on connection failure event test`() {
        val error = Throwable("timeout")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnFailure(error, "peerId")))
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState &&
                    secondValue is OnError
        }
    }

    @Test
    fun `on connection error event test`() {
        val error = Throwable("timeout")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.error(error))
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue is OnError
        }
    }

    @Test
    fun `on disconnect event test`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnDisconnect))
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
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"))))
        NetworkManager.networks = listOf(Network(full = "Ethereum", chainId = 1))
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequestWithDefinedNetwork(meta, "Ethereum")
        }
    }

    @Test
    fun `on session request event test with not defined chainId on test net`() {
        val meta = WalletConnectPeerMeta(name = "token", url = "url", description = "dsc")
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
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.account = Account(1, networkShort = "eth_goerli")
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo
                    OnSessionRequestWithUndefinedNetwork(meta, "Ethereum (Görli)")
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
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.setConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState
            secondValue shouldBeEqualTo
                    OnSessionRequestWithUndefinedNetwork(meta, "Ethereum")
            viewModel.requestedNetwork shouldBe "Ethereum"
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
                    full = "Ethereum",
                    chainId = 1,
                    short = "eth_mainnet",
                    testNet = false,
                    httpRpc = "url"
                )
            )
        )
        viewModel.topic = Topic()
        viewModel.currentSession = WalletConnectSession("topic", "version", "bridge", "key")
        viewModel.account = Account(1, networkShort = "eth_mainnet")
        whenever(repository.approveSession(any(), any(), any(), any())).thenReturn(Completable.complete())
        viewModel.approveSession(WalletConnectPeerMeta(name = "name", url = "url"))
        verify(repository).approveSession(any(), any(), any(), any())
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