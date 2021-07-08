package minerva.android.accounts.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.transaction.fragment.scanner.AddressParser
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.accounts.walletconnect.WalletConnectScannerFragment.Companion.FIRST_ICON
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.exception.InvalidAccountThrowable
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.Topic
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnFailure
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import timber.log.Timber

class WalletConnectViewModel(
    private val accountManager: AccountManager,
    private val repository: WalletConnectRepository,
    private val logger: Logger
) : BaseViewModel() {

    internal lateinit var account: Account
    var requestedNetwork: String = String.Empty
    internal lateinit var topic: Topic
    private var handshakeId: Long = 0L
    internal lateinit var currentSession: WalletConnectSession

    private val _viewStateLiveData = MutableLiveData<WalletConnectState>()
    val stateLiveData: LiveData<WalletConnectState> get() = _viewStateLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    fun subscribeToConnectionStatusFlowable() {
        launchDisposable {
            repository.connectionStatusFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { _viewStateLiveData.value = ProgressBarState(false) }
                .subscribeBy(
                    onNext = { status ->
                        _viewStateLiveData.value = when (status) {
                            is OnSessionRequest -> {
                                topic = status.topic
                                handshakeId = status.handshakeId
                                handleSessionRequest(status)
                            }
                            is OnDisconnect -> OnDisconnected(status.sessionName)
                            is OnFailure -> {
                                logger.logToFirebase("OnWalletConnectConnectionError: ${status.error}")
                                OnWalletConnectConnectionError(status.error, status.sessionName)
                            }
                            else -> DefaultRequest
                        }
                    },
                    onError = {
                        logger.logToFirebase("WalletConnect statuses error: $it")
                        _errorLiveData.value = Event(it)
                    }
                )
        }
    }

    fun getAccount(index: Int) {
        if (index != Int.InvalidIndex) {
            account = accountManager.loadAccount(index)
            launchDisposable {
                repository.getSessionsFlowable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onNext = {
                            _viewStateLiveData.value = if (it.isEmpty()) {
                                HideDappsState
                            } else {
                                UpdateDappsState(it)
                            }

                        },
                        onError = {
                            _viewStateLiveData.value = OnGeneralError(it)
                        }
                    )
            }
        } else {
            _viewStateLiveData.value = OnGeneralError(InvalidAccountThrowable())
        }
    }

    fun removeDeadSession() {
        repository.removeDeadSessions()
        _viewStateLiveData.value = ProgressBarState(false)
    }

    fun closeScanner() {
        _viewStateLiveData.value = CloseScannerState
    }

    fun rejectSession() {
        repository.rejectSession(topic.peerId)
    }

    fun killSession(peerId: String) {
        launchDisposable {
            repository.killSession(peerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _viewStateLiveData.value = OnSessionDeleted },
                    onError = { Timber.e(it) })
        }
    }

    fun handleQrCode(qrCode: String) {
        if (AddressParser.parse(qrCode) != WALLET_CONNECT) {
            _viewStateLiveData.value = WrongQrCodeState
        } else {
            _viewStateLiveData.value = CorrectQrCodeState
            currentSession = repository.getWCSessionFromQr(qrCode)
            repository.connect(currentSession)
        }
    }

    fun approveSession(meta: WalletConnectPeerMeta) {
        launchDisposable {
            val chainId = account.network.chainId
            repository.approveSession(listOf(account.address), chainId, topic.peerId, getDapp(meta, chainId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { closeScanner() },
                    onError = { error -> _errorLiveData.value = Event(error) })
        }
    }

    private fun getDapp(meta: WalletConnectPeerMeta, chainId: Int) = DappSession(
        account.address,
        currentSession.topic,
        currentSession.version,
        currentSession.bridge,
        currentSession.key,
        meta.name,
        getIcon(meta.icons),
        topic.peerId,
        topic.remotePeerId,
        requestedNetwork,
        account.name,
        chainId,
        handshakeId
    )

    private fun getIcon(icons: List<String>) =
        if (icons.isEmpty()) String.Empty
        else icons[FIRST_ICON]

    private fun handleSessionRequest(sessionRequest: OnSessionRequest): WalletConnectState =
        sessionRequest.chainId?.let { id ->
            requestedNetwork = getNetworkName(id)
            OnSessionRequest(sessionRequest.meta, requestedNetwork, getAlertType(sessionRequest.meta.url))
        }.orElse {
            requestedNetwork = account.network.name
            OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.UNDEFINED_NETWORK_WARNING)
        }

    private fun getAlertType(url: String) = when {
        account.network.name == requestedNetwork -> WalletConnectAlertType.NO_ALERT
        requestedNetwork == getNetworkName(ETHEREUM_CHAIN_ID) && isUrlContainAccountNetwork(url) -> WalletConnectAlertType.NO_ALERT
        requestedNetwork == getNetworkName(ETHEREUM_CHAIN_ID) && !isUrlContainAccountNetwork(url) -> WalletConnectAlertType.WARNING
        account.network.name != requestedNetwork -> WalletConnectAlertType.ERROR
        else -> WalletConnectAlertType.NO_ALERT
    }

    private fun isUrlContainAccountNetwork(url: String): Boolean = url.contains(account.network.token, true)

    private fun getNetworkName(chainId: Int): String =
        NetworkManager.networks.find { network -> network.chainId == chainId }?.name.orElse { String.Empty }

    companion object {
        private const val ETHEREUM_CHAIN_ID = 1
    }
}