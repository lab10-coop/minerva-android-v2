package minerva.android.accounts.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.transaction.fragment.scanner.AddressParser
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletConnect.client.OnConnectionFailure
import minerva.android.walletConnect.client.OnDisconnect
import minerva.android.walletConnect.client.OnSessionRequest
import minerva.android.walletConnect.repository.WalletConnectRepository
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.defs.NetworkShortName

class WalletConnectViewModel(
    private val repository: WalletConnectRepository,
    private val accountManager: AccountManager
) : BaseViewModel() {

    //TODO get list of connected dapps
    val dapps: MutableList<Dapp> = mutableListOf()

    private lateinit var account: Account

    private val _viewStateLiveData = MutableLiveData<WalletConnectViewState>()
    val viewStateLiveData: LiveData<WalletConnectViewState> get() = _viewStateLiveData

    init {
        launchDisposable {
            repository.connectionStatusFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { _viewStateLiveData.value = ProgressBarState(false) }
                .subscribeBy(
                    onNext = {
                        _viewStateLiveData.value = when (it) {
                            is OnSessionRequest -> handleSessionRequest(it)
                            is OnConnectionFailure -> OnError(it.error)
                            is OnDisconnect -> OnDisconnected(it.reason)
                        }
                    },
                    onError = {
                        _viewStateLiveData.value = OnError(it)
                    }
                )
        }
    }

    private fun handleSessionRequest(it: OnSessionRequest) =
        it.chainId?.let { id ->
            OnSessionRequestWithDefinedNetwork(it.meta, getNetworkName(id))
        }.orElse {
            OnSessionRequestWithUndefinedNetwork(it.meta, getNetworkName(it.chainId))
        }

    fun getAccount(index: Int) {
        account = accountManager.loadAccount(index)
    }

    fun closeScanner() {
        _viewStateLiveData.value = CloseScannerState
    }

    fun rejectSession() {
        repository.rejectSession("Rejected by user")
    }

    fun killSession() {
        repository.killSession()
    }

    fun handleQrCode(qrCode: String) {
        if (AddressParser.parse(qrCode) != WALLET_CONNECT) {
            _viewStateLiveData.value = WrongQrCodeState
        } else {
            _viewStateLiveData.value = CorrectQrCodeState
            repository.connect(qrCode)
        }
    }

    fun approveSession() {
        repository.approveSession(listOf(account.address), account.network.chainId)
    }

    private fun getNetworkName(chainId: Int?): String {
        chainId?.let {
            return NetworkManager.networks.find { it.chainId == chainId }?.full.orElse { getNetworkWhenChainIdNotDefined() }
        }.orElse {
            return getNetworkWhenChainIdNotDefined()
        }
    }

    private fun getNetworkWhenChainIdNotDefined(): String =
        if (account.network.testNet) {
            NetworkManager.networks.find { it.short == NetworkShortName.ETH_GOR }?.full.toString()
        } else {
            NetworkManager.networks.find { it.short == NetworkShortName.ETH_MAIN }?.full.toString()
        }
}