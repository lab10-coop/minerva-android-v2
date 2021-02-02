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
import minerva.android.utils.exhaustive
import minerva.android.walletmanager.exception.InvalidAccountException
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.repository.walletconnect.OnConnectionFailure
import minerva.android.walletmanager.repository.walletconnect.OnDisconnect
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import timber.log.Timber

class WalletConnectViewModel(
    private val accountManager: AccountManager,
    private val repository: WalletConnectRepository
) : BaseViewModel() {

    internal lateinit var account: Account
    var requestedNetwork: String = String.Empty
    internal lateinit var topic: Topic
    internal lateinit var currentSession: WalletConnectSession

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
                            is OnConnectionFailure -> OnError(it.error)
                            is OnDisconnect -> OnDisconnected
                            else -> OnError(Throwable()) //todo delete
                        }
                    },
                    onError = {
                        _viewStateLiveData.value = OnError(it)
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
                        onError = { _viewStateLiveData.value = OnError(it) }
                    )
            }
        } else {
            _viewStateLiveData.value = OnError(InvalidAccountException())
        }
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
            repository.approveSession(listOf(account.address), account.network.chainId, topic.peerId, getDapp(meta))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { OnError(it) })
        }
    }

    private fun getDapp(meta: WalletConnectPeerMeta) = DappSession(
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