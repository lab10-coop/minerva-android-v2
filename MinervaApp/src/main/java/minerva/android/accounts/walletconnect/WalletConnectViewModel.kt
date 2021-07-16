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
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.exception.InvalidAccountThrowable
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.ChainId
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

    val availableNetworks: List<NetworkDataSpinnerItem> get() = prepareAvailableNetworks()
    private val baseNetwork get() = if (accountManager.areMainNetworksEnabled) ChainId.ETH_MAIN else ChainId.ETH_GOR

    private fun prepareAvailableNetworks(): List<NetworkDataSpinnerItem> =
        mutableListOf<NetworkDataSpinnerItem>().apply {
            val availableAccountList = accountManager.getFirstActiveAccountForAllNetworks()
            add(
                Int.FirstIndex,
                NetworkDataSpinnerItem(
                    getNetworkName(baseNetwork),
                    baseNetwork,
                    availableAccountList.find { account -> account.chainId == baseNetwork } != null))
            addAll(availableAccountList.filter { account -> account.chainId != baseNetwork }
                .map { account -> NetworkDataSpinnerItem(account.network.name, account.chainId) }
            )
        }

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

    private fun handleSessionRequest(sessionRequest: OnSessionRequest): WalletConnectState {
        val id = sessionRequest.chainId
        return when {
            id == null -> OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.UNDEFINED_NETWORK_WARNING)
            isNetworkNotSupported(chainId = id) -> OnSessionRequest(
                sessionRequest.meta,
                id.toString(),
                WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
            )
            account.chainId != id -> getWalletConnectStateForNotEqualNetworks(sessionRequest, id)
            else -> {
                requestedNetwork = getNetworkName(id)
                OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.NO_ALERT)
            }
        }

    }

    private fun getWalletConnectStateForNotEqualNetworks(sessionRequest: OnSessionRequest, chainId: Int): WalletConnectState {
        requestedNetwork = getNetworkName(chainId)
        return accountManager.getFirstActiveAccountOrNull(chainId)?.let { newAccount ->
            account = newAccount
            OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.CHANGE_ACCOUNT_WARNING)
        }.orElse {
            OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR)
        }
    }

    private fun getNetworkName(chainId: Int): String =
        NetworkManager.networks.find { network -> network.chainId == chainId }?.name.orElse { String.Empty }

    private fun isNetworkNotSupported(chainId: Int): Boolean =
        NetworkManager.networks.find { network -> network.chainId == chainId } == null

    fun setAccountForSelectedNetwork(chainId: Int) {
        accountManager.getFirstActiveAccountOrNull(chainId)?.let { newAccount ->
            account = newAccount
        }
    }
}