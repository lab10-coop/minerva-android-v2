package minerva.android.accounts.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.transaction.fragment.scanner.AddressParser
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletConnect.client.OnConnectionFailure
import minerva.android.walletConnect.client.OnDisconnect
import minerva.android.walletConnect.client.OnSessionRequest
import minerva.android.walletConnect.model.session.Dapp
import minerva.android.walletConnect.model.session.Topic
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.repository.WalletConnectRepository
import minerva.android.walletConnect.storage.WalletConnectStorage
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.defs.NetworkShortName

class WalletConnectViewModel(
    private val repository: WalletConnectRepository,
    private val accountManager: AccountManager,
    private val storage: WalletConnectStorage
) : BaseViewModel() {

    internal lateinit var account: Account
    var requestedNetwork: String = String.Empty
    private lateinit var topic: Topic

    private val _viewStateLiveData = MutableLiveData<WalletConnectViewState>()
    val viewStateLiveData: LiveData<WalletConnectViewState> get() = _viewStateLiveData

    lateinit var connectedDapps: MutableList<Dapp>

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
                            is OnDisconnect -> onDisconnected(it)
                        }
                    },
                    onError = {
                        _viewStateLiveData.value = OnError(it)
                    }
                )
        }
    }

    fun getAccount(index: Int) {
        account = accountManager.loadAccount(index)
//        storage.clear()
        connectedDapps = storage.getConnectedDapps(account.address).toMutableList()
        //todo try to reconnect
    }

    fun closeScanner() {
        _viewStateLiveData.value = CloseScannerState
    }

    fun rejectSession() {
        repository.rejectSession(topic.peerId)
    }

    fun killSession(peerId: String) {
        repository.killSession(peerId)
    }

    fun handleQrCode(qrCode: String) {
        if (AddressParser.parse(qrCode) != WALLET_CONNECT) {
            _viewStateLiveData.value = WrongQrCodeState
        } else {
            _viewStateLiveData.value = CorrectQrCodeState
            repository.connect(qrCode)
        }
    }

    fun approveSession(meta: WCPeerMeta) {
        repository.approveSession(listOf(account.address), account.network.chainId, topic.peerId)
        val dapp = Dapp(meta.name, getIcon(meta.icons), topic.peerId, topic.remotePeerId)
        storage.saveDapp(account.address, dapp)
        connectedDapps.add(dapp)
        //todo work only on local storage // should post to live data and update view
    }

    private fun onDisconnected(it: OnDisconnect): OnDisconnected {
        connectedDapps.find { dApp -> dApp.peerId == it.peerId }
            ?.let {
                storage.removeDapp(account.address, it)
                connectedDapps.remove(it)
            }
        //todo work only on local storage // should post to live data and update view
        return OnDisconnected(it.reason, it.peerId)
    }

    private fun getIcon(icons: List<String>) =
        if (icons.isEmpty()) String.Empty
        else icons[WalletConnectScannerFragment.FIRST_ICON]

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