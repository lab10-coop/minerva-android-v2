package minerva.android.walletconnect

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.walletconnect.*
import minerva.android.extension.empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.walletconnect.*
import minerva.android.walletmanager.provider.UnsupportedNetworkRepository
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WalletConnectViewModelTest : BaseViewModelTest() {

    private val repository: WalletConnectRepository = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val manager: AccountManager = mock()
    private val logger: Logger = mock()
    private lateinit var viewModel: WalletConnectViewModel
    private val stateObserver: Observer<WalletConnectState> = mock()
    private val stateCaptor: KArgumentCaptor<WalletConnectState> = argumentCaptor()
    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()
    private val meta = WalletConnectPeerMeta(name = "token", url = "test.xdai.com", description = "dsc")
    private val unsupportedNetworkRepository: UnsupportedNetworkRepository =mock()


    @Before
    fun setup() {
        viewModel = WalletConnectViewModel(manager, repository, logger, walletActionsRepository,unsupportedNetworkRepository)
    }

    @Test
    fun `on connection failure event test`() {
        val error = Throwable("timeout")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `on disconnect event test`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnDisconnect()))
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
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
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        viewModel.account = Account(1, chainId = 1)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(meta, BaseNetworkData(1, "Ethereum"), WalletConnectAlertType.NO_ALERT)
        }
    }

    @Test
    fun `on session request event test with defined chainId on test net with xDai account, Ethereum dapp and no Ethereum account`() {
        val meta = WalletConnectPeerMeta(name = "token", url = "test.com", description = "dsc")
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks =
            listOf(Network(name = "xDai", chainId = 2, token = "xDai"), Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        whenever(manager.getFirstActiveAccountOrNull(1)).thenReturn(null)
        viewModel.account = Account(1, chainId = 2)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(
                meta,
                BaseNetworkData(1, "Ethereum"),
                WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR
            )
        }
    }

    @Test
    fun `on session request event test with defined chainId on test net with xDai account, Ethereum dapp and Ethereum account exist`() {
        val meta = WalletConnectPeerMeta(name = "token", url = "test.com", description = "dsc")
        val ethereumAccount = Account(1, chainId = 1)
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks =
            listOf(Network(name = "xDai", chainId = 2, token = "xDai"), Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        whenever(manager.getFirstActiveAccountOrNull(1)).thenReturn(ethereumAccount)
        viewModel.account = Account(1, chainId = 2)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(
                meta,
                BaseNetworkData(1, "Ethereum"),
                WalletConnectAlertType.CHANGE_ACCOUNT_WARNING
            )
        }
        viewModel.account shouldBeEqualTo ethereumAccount
    }

    @Test
    fun `on session request event test with defined but unsupported chainId`() {
        whenever(repository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 5, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks =
            listOf(Network(name = "xDai", chainId = 2, token = "xDai"), Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        whenever(unsupportedNetworkRepository.getNetworkName(5)).thenReturn(Single.just("networkname"))
        viewModel.account = Account(1, chainId = 1)
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(3)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(
                meta,
                BaseNetworkData(5, String.empty),
                WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
            )
            thirdValue shouldBeEqualTo UpdateOnSessionRequest(
                BaseNetworkData(5, "networkname"),
                WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
            )
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
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo OnSessionRequest(
                meta,
                BaseNetworkData(Int.InvalidId, String.empty),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
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
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState
            secondValue shouldBeEqualTo OnSessionRequest(
                meta,
                BaseNetworkData(Int.InvalidId, String.empty),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
            viewModel.requestedNetwork shouldBeEqualTo BaseNetworkData(Int.InvalidId, String.empty)
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
            firstValue shouldBe CorrectWalletConnectCodeState
        }
    }

    @Test
    fun `handle not wc qr code test`() {
        viewModel.stateLiveData.observeForever(stateObserver)
        viewModel.handleQrCode("qr:123456789")
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue shouldBe WrongWalletConnectCodeState
        }
    }

    @Test
    fun `approve session test`() {
        NetworkManager.initialize(listOf(Network(name = "Ethereum", chainId = 2, testNet = false, httpRpc = "url")))
        viewModel.topic = Topic()
        viewModel.currentSession = WalletConnectSession("topic", "version", "bridge", "key")
        viewModel.account = Account(1, chainId = 2)
        viewModel.stateLiveData.observeForever(stateObserver)
        whenever(repository.approveSession(any(), any(), any(), any())).thenReturn(Completable.complete())
        viewModel.approveSession(WalletConnectPeerMeta(name = "name", url = "url", isMobileWalletConnect = false))
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

    @Test
    fun `get available for test networks test`() {
        NetworkManager.initialize(
            listOf(
                Network(name = "Groli", chainId = 5, testNet = true, httpRpc = "url"),
                Network(name = "xDai", chainId = 2, testNet = true, httpRpc = "url")
            )
        )
        whenever(manager.areMainNetworksEnabled).thenReturn(false)
        whenever(manager.getFirstActiveAccountForAllNetworks()).thenReturn(
            listOf(
                Account(1, chainId = 5, address = "address1", _isTestNetwork = true, isHide = false),
                Account(1, chainId = 2, address = "address1", _isTestNetwork = true, isHide = false)
            )
        )
        viewModel.availableNetworks shouldBeEqualTo listOf(
            NetworkDataSpinnerItem("Groli", 5), NetworkDataSpinnerItem("xDai", 2)
        )

        whenever(manager.getFirstActiveAccountForAllNetworks()).thenReturn(
            listOf(Account(1, chainId = 2, address = "address1", _isTestNetwork = true, isHide = false))
        )
        viewModel.availableNetworks shouldBeEqualTo listOf(
            NetworkDataSpinnerItem("Groli", 5, false), NetworkDataSpinnerItem("xDai", 2)
        )
    }

    @Test
    fun `get available for main networks test`() {
        NetworkManager.initialize(
            listOf(
                Network(name = "Ethereum", chainId = 1, testNet = false, httpRpc = "url"),
                Network(name = "xDai", chainId = 2, testNet = false, httpRpc = "url")
            )
        )
        whenever(manager.areMainNetworksEnabled).thenReturn(true)
        whenever(manager.getFirstActiveAccountForAllNetworks()).thenReturn(
            listOf(
                Account(1, chainId = 2, address = "address1", _isTestNetwork = false, isHide = false),
                Account(1, chainId = 1, address = "address1", _isTestNetwork = false, isHide = false)
            )
        )
        viewModel.availableNetworks shouldBeEqualTo listOf(
            NetworkDataSpinnerItem("Ethereum", 1), NetworkDataSpinnerItem("xDai", 2)
        )

        whenever(manager.getFirstActiveAccountForAllNetworks()).thenReturn(
            listOf(Account(1, chainId = 2, address = "address1", _isTestNetwork = false, isHide = false))
        )
        viewModel.availableNetworks shouldBeEqualTo listOf(
            NetworkDataSpinnerItem("Ethereum", 1, false), NetworkDataSpinnerItem("xDai", 2)
        )
    }

    @Test
    fun `set account for selected network test`() {
        val account = Account(1, chainId = 1, address = "address1", _isTestNetwork = false, isHide = false)
        whenever(manager.getFirstActiveAccountOrNull(1)).thenReturn(account)
        viewModel.setAccountForSelectedNetwork(1)
        viewModel.account shouldBeEqualTo account
    }

    @Test
    fun `set selected account test`() {
        val account = Account(1, chainId = 1, address = "address123", _isTestNetwork = false, isHide = false)
        viewModel.setNewAccount(account)
        viewModel.account shouldBeEqualTo account
    }

    @Test
    fun `set get all accounts for selected network test`() {
        val account = Account(1, chainId = 1, address = "address123")
        val accounts = listOf(
            Account(1, chainId = 1, address = "address121"),
            Account(2, chainId = 1, address = "address122"),
            Account(3, chainId = 1, address = "address123")
        )
        whenever(manager.getAllActiveAccounts(1)).thenReturn(accounts)
        viewModel.setNewAccount(account)
        viewModel.availableAccounts shouldBeEqualTo accounts
    }

    @Test
    fun `check Creating new Account flow`() {
        NetworkManager.initialize(listOf(Network(chainId = 3, name = "xDai", httpRpc = "some_rpc")))
        whenever(manager.createOrUnhideAccount(any())).thenReturn(Single.just("Cookie Account"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.run {
            stateLiveData.observeForever(stateObserver)
            errorLiveData.observeForever(errorObserver)
            requestedNetwork = BaseNetworkData(3, "xDai")
            addAccount(3, WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR)
            addAccount(3, WalletConnectAlertType.UNDEFINED_NETWORK_WARNING)
        }

        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo UpdateOnSessionRequest(BaseNetworkData(3, "xDai"), WalletConnectAlertType.NO_ALERT)
            secondValue shouldBeEqualTo UpdateOnSessionRequest(
                BaseNetworkData(3, "xDai"),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
        }
    }
}