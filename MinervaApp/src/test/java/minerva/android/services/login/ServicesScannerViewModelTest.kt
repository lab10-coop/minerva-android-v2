package minerva.android.services.login

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.walletconnect.NetworkDataSpinnerItem
import minerva.android.accounts.walletconnect.WalletConnectAlertType
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.Topic
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
import minerva.android.walletmanager.provider.UnsupportedNetworkRepository
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class ServicesScannerViewModelTest : BaseViewModelTest() {

    private val walletConnectRepository: WalletConnectRepository = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val accountManager: AccountManager = mock()
    private val logger: Logger = mock()
    private val serviceManager: ServiceManager = mock()
    private val identityManager: IdentityManager = mock()
    private val unsupportedNetworkRepository: UnsupportedNetworkRepository = mock()

    private lateinit var viewModel: ServicesScannerViewModel

    private val stateObserver: Observer<ServicesScannerViewState> = mock()
    private val stateCaptor: KArgumentCaptor<ServicesScannerViewState> = argumentCaptor()

    private val meta = WalletConnectPeerMeta(name = "token", url = "test.xdai.com", description = "dsc")

    @Before
    fun setup() {
        viewModel = ServicesScannerViewModel(
            serviceManager,
            walletActionsRepository,
            walletConnectRepository,
            accountManager,
            logger,
            identityManager,
            unsupportedNetworkRepository
        )
    }

    @Test
    fun `on connection failure event test`() {
        val error = Throwable("timeout")
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.error(error))
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue is Error
        }
    }

    @Test
    fun `on disconnect event test`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnDisconnect()))
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue is ProgressBarState &&
                    secondValue is WalletConnectDisconnectResult
        }
    }

    @Test
    fun `on session request event with undefined network warning test`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, null, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo WalletConnectSessionRequestResult(
                meta,
                BaseNetworkData(Int.InvalidId, String.Empty),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `on session request event with not supported network test`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 5, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        whenever(unsupportedNetworkRepository.getNetworkName(5)).thenReturn(Single.just("networkname"))
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(3)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo WalletConnectSessionRequestResult(
                meta,
                BaseNetworkData(5,String.Empty),
                WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
            )
            thirdValue shouldBeEqualTo WalletConnectUpdateDataState(
                BaseNetworkData(5, "networkname"),
                WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `on session request event with no available account test`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        //set ethereum account which has to return unknown accounts
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo WalletConnectSessionRequestResult(
                meta,
                BaseNetworkData(Int.InvalidId, String.Empty),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `on session request event with change account warning test`() {
        whenever(walletConnectRepository.connectionStatusFlowable)
            .thenReturn(Flowable.just(OnSessionRequest(meta, 1, Topic("peerID", "remotePeerID"), 1)))
        NetworkManager.networks = listOf(Network(name = "Ethereum", chainId = 1, token = "Ethereum"))
        val ethereumAccount = Account(1, chainId = 1)//ethereum account has to return unknown accounts
        whenever(accountManager.getFirstActiveAccountOrNull(1)).thenReturn(ethereumAccount)
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.subscribeToWCConnectionStatusFlowable()
        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo ProgressBarState(false)
            secondValue shouldBeEqualTo WalletConnectSessionRequestResult(
                meta,
                BaseNetworkData(Int.InvalidId, String.Empty),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
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
        whenever(accountManager.areMainNetworksEnabled).thenReturn(false)
        whenever(accountManager.getFirstActiveAccountForAllNetworks()).thenReturn(
            listOf(
                Account(1, chainId = 5, address = "address1", _isTestNetwork = true, isHide = false),
                Account(1, chainId = 2, address = "address1", _isTestNetwork = true, isHide = false)
            )
        )
        viewModel.availableNetworks shouldBeEqualTo listOf(
            NetworkDataSpinnerItem("Groli", 5), NetworkDataSpinnerItem("xDai", 2)
        )

        whenever(accountManager.getFirstActiveAccountForAllNetworks()).thenReturn(
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
        whenever(accountManager.areMainNetworksEnabled).thenReturn(true)
        whenever(accountManager.getFirstActiveAccountForAllNetworks()).thenReturn(
            listOf(
                Account(1, chainId = 2, address = "address1", _isTestNetwork = false, isHide = false),
                Account(1, chainId = 1, address = "address1", _isTestNetwork = false, isHide = false)
            )
        )
        viewModel.availableNetworks shouldBeEqualTo listOf(
            NetworkDataSpinnerItem("Ethereum", 1), NetworkDataSpinnerItem("xDai", 2)
        )

        whenever(accountManager.getFirstActiveAccountForAllNetworks()).thenReturn(
            listOf(Account(1, chainId = 2, address = "address1", _isTestNetwork = false, isHide = false))
        )
        viewModel.availableNetworks shouldBeEqualTo listOf(
            NetworkDataSpinnerItem("Ethereum", 1, false), NetworkDataSpinnerItem("xDai", 2)
        )
    }

    @Test
    fun `set account for selected network test`() {
        val account = Account(1, chainId = 1, address = "address1", _isTestNetwork = false, isHide = false)
        whenever(accountManager.getFirstActiveAccountOrNull(1)).thenReturn(account)
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
        whenever(accountManager.getAllActiveAccounts(1)).thenReturn(accounts)
        viewModel.setNewAccount(account)
        viewModel.availableAccounts shouldBeEqualTo accounts
    }

    @Test
    fun `creating new account test`() {
        NetworkManager.initialize(listOf(Network(chainId = 3, name = "xDai", httpRpc = "some_rpc")))
        whenever(accountManager.createOrUnhideAccount(any())).thenReturn(Single.just("Cookie Account"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.run {
            viewStateLiveData.observeForever(stateObserver)
            requestedNetwork = BaseNetworkData(3, "xDai")
            addAccount(3, WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR)
            addAccount(3, WalletConnectAlertType.UNDEFINED_NETWORK_WARNING)
        }

        stateCaptor.run {
            verify(stateObserver, times(2)).onChanged(capture())
            firstValue shouldBeEqualTo WalletConnectUpdateDataState(BaseNetworkData(3, "xDai"), WalletConnectAlertType.NO_ALERT)
            secondValue shouldBeEqualTo WalletConnectUpdateDataState(
                BaseNetworkData(3, "xDai"),
                WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
            )
        }
    }

    @Test
    fun `approve session test with close app state`() {
        NetworkManager.initialize(listOf(Network(name = "Ethereum", chainId = 2, testNet = false, httpRpc = "url")))
        viewModel.topic = Topic()
        viewModel.currentSession = WalletConnectSession("topic", "version", "bridge", "key")
        viewModel.account = Account(1, chainId = 2)
        viewModel.viewStateLiveData.observeForever(stateObserver)
        whenever(walletConnectRepository.approveSession(any(), any(), any(), any())).thenReturn(Completable.complete())
        viewModel.approveSession(WalletConnectPeerMeta(name = "name", url = "url", isMobileWalletConnect = false))
        verify(walletConnectRepository).approveSession(any(), any(), any(), any())
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue is CloseScannerState
        }
    }

    @Test
    fun `reject session test`() {
        viewModel.topic = Topic()
        viewModel.rejectSession()
        verify(walletConnectRepository).rejectSession("")
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
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.validateResult("wc:123456789")
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue shouldBe CorrectWalletConnectResult
        }
    }

    @Test
    fun `handle login qr code test`() {
        whenever(serviceManager.decodeJwtToken(any())).thenReturn(Single.just(ServiceQrCode("Minerva App")))
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.validateResult("token")
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            (firstValue as ServiceLoginResult).let { value ->
                value.qrCode.serviceName == "MinervaApp"
            }
        }
    }

    @Test
    fun `bind credential qr code to identity success test`() {
        whenever(serviceManager.decodeJwtToken(any())).thenReturn(
            Single.just(
                CredentialQrCode(
                    "Minerva App",
                    loggedInDid = "did"
                )
            )
        )
        whenever(identityManager.bindCredentialToIdentity(any())).thenReturn(Single.just("name"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(identityManager.isCredentialLoggedIn(any())).doReturn(false)
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.validateResult("token")
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            (firstValue as CredentialsLoginResult).let { value ->
                value.message == "name"
            }
        }
    }

    @Test
    fun `update credential qr code success test`() {
        whenever(serviceManager.decodeJwtToken(any())).thenReturn(
            Single.just(
                CredentialQrCode(
                    "Minerva App",
                    loggedInDid = "did"
                )
            )
        )
        whenever(identityManager.isCredentialLoggedIn(any())).doReturn(true)
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.validateResult("token")
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            firstValue shouldBeInstanceOf UpdateCredentialsLoginResult::class.java
        }
    }

    @Test
    fun `validate qr code result failed test`() {
        val error = Throwable()
        whenever(serviceManager.decodeJwtToken(any())).thenReturn(Single.error(error))
        viewModel.viewStateLiveData.observeForever(stateObserver)
        viewModel.validateResult("token")
        stateCaptor.run {
            verify(stateObserver).onChanged(capture())
            (firstValue as Error).error == error
        }
    }
}