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
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletConnect.client.OnConnectionFailure
import minerva.android.walletConnect.client.OnDisconnect
import minerva.android.walletConnect.client.OnSessionRequest
import minerva.android.walletConnect.model.exceptions.InvalidAccountException
import minerva.android.walletConnect.model.session.Topic
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletConnect.repository.WalletConnectRepository
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.repository.walletconnect.DappSessionRepository
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.defs.NetworkShortName

class WalletConnectViewModel(
    private val sessionRepository: DappSessionRepository, //todo tylko do getSessionsFlowable
    private val accountManager: AccountManager,
    private val repository: WalletConnectRepository
) : BaseViewModel() {

    internal lateinit var account: Account
    var requestedNetwork: String = String.Empty
    internal lateinit var topic: Topic
    internal lateinit var currentSession: WCSession

    private val _viewStateLiveData = MutableLiveData<WalletConnectViewState>()
    val viewStateLiveData: LiveData<WalletConnectViewState> get() = _viewStateLiveData

    fun setConnectionStatusFlowable() {
        launchDisposable {
            repository.connectionStatusFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { _viewStateLiveData.value = ProgressBarState(false) }
                .subscribeBy(
                    onNext = {
                        _viewStateLiveData.value = when (it) {
                            is OnSessionRequest -> {
                                topic = it.topic
                                handleSessionRequest(it)
                            }
                            is OnConnectionFailure -> onError(it)
                            is OnDisconnect -> onDisconnected(it)
                        }
                    },
                    onError = {
                        _viewStateLiveData.value = OnError(it)
                    }
                )
        }
    }

    private fun onError(it: OnConnectionFailure): OnError {
        if (!repository.walletConnectClients.containsKey(it.peerId)) {
            it.peerId?.let {
                //todo remove session powinno byc na posiomie servisu i tam usuwac sessje z bazy a tu tylko reagowac w UI na event
                removeSession(it)
            }
        }
        return OnError(it.error)
    }

    fun getAccount(index: Int) {
        if (index != Int.InvalidIndex) {
            account = accountManager.loadAccount(index)
            launchDisposable {
                sessionRepository.getSessionsFlowable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onNext = {
                            _viewStateLiveData.value = if (it.isEmpty()) {
                                HideDappsState
                            } else {
                                reconnect(it) //todo remove reconnection, recconnection will be done when service binded to main activity is started
                                UpdateDappsState(it)
                            }

                        },
                        onError = { _viewStateLiveData.value = OnError(it) }
                    )
            }
        } else {
            _viewStateLiveData.value = OnError(InvalidAccountException())
        }
    }

    private fun reconnect(dapps: List<DappSession>) {
        if (repository.isClientMapEmpty) {
            dapps.forEach { session ->
                with(session) {
                    repository.connect(WCSession(topic, version, bridge, key), peerId, remotePeerId)
                }
            }
        }
    }

    fun closeScanner() {
        _viewStateLiveData.value = CloseScannerState
    }

    fun rejectSession() {
        repository.rejectSession(topic.peerId)
    }

    fun killSession(peerId: String) {
        repository.killSession(peerId)
        removeSession(peerId) //todo usuwanie sesji z bazy w servisie powinno byc
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

    fun approveSession(meta: WCPeerMeta) {
        repository.approveSession(listOf(account.address), account.network.chainId, topic.peerId)
        launchDisposable {
            //todo nope dodawanie do bazy powinno byc w servisie jak zaraz po approveSession, gdzie przekazuje DappSession
            sessionRepository.saveDappSession(getDapp(meta))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { OnError(it) })
        }
    }

    private fun getDapp(meta: WCPeerMeta) = DappSession(
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
        account.name
    )

    private fun onDisconnected(it: OnDisconnect): OnDisconnected {
        it.peerId?.let { peerId ->
            if (repository.walletConnectClients.containsKey(peerId)) {
                removeSession(peerId) //todo przenies to servisu tutaj tylko update UI
            }
        }
        return OnDisconnected
    }

    private fun removeSession(peerId: String) {
        launchDisposable {
            //toto tutaj powinien byc serwis, gdzie jest robione killSession, a potem usuwanie z db juz w serwisie
            sessionRepository.deleteDappSession(peerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { OnError(it) })
        }
    }

    private fun getIcon(icons: List<String>) =
        if (icons.isEmpty()) String.Empty
        else icons[FIRST_ICON]

    val shouldChangeNetwork: Boolean
        get() = account.network.full != requestedNetwork

    private fun handleSessionRequest(it: OnSessionRequest): WalletConnectViewState =
        it.chainId?.let { id ->
            requestedNetwork = getNetworkName(id).orElse { String.Empty }
            OnSessionRequestWithDefinedNetwork(it.meta, requestedNetwork)
        }.orElse {
            requestedNetwork = getNetworkName(it.chainId).orElse { String.Empty }
            OnSessionRequestWithUndefinedNetwork(it.meta, requestedNetwork)
        }

    private fun getNetworkName(chainId: Int?): String? {
        chainId?.let {
            return NetworkManager.networks.find { it.chainId == chainId }?.full.orElse { getNetworkWhenChainIdNotDefined() }
        }.orElse {
            return getNetworkWhenChainIdNotDefined()
        }
    }

    private fun getNetworkWhenChainIdNotDefined(): String? =
        if (account.network.testNet) {
            NetworkManager.networks.find { it.short == NetworkShortName.ETH_GOR }?.full
        } else {
            NetworkManager.networks.find { it.short == NetworkShortName.ETH_MAIN }?.full
        }
}