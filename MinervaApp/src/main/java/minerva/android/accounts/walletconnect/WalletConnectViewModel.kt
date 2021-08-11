package minerva.android.accounts.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.transaction.fragment.scanner.AddressParser
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.extension.empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.exception.InvalidAccountThrowable
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class WalletConnectViewModel(
    private val accountManager: AccountManager,
    private val repository: WalletConnectRepository,
    logger: Logger,
    walletActionsRepository: WalletActionsRepository
) : BaseWalletConnectScannerViewModel(accountManager, repository, logger, walletActionsRepository) {

    private val _viewStateLiveData = MutableLiveData<WalletConnectState>()
    val stateLiveData: LiveData<WalletConnectState> get() = _viewStateLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    override lateinit var account: Account
    override val selectedChainId: Int
        get() = account.chainId

    override fun hideProgress() {
        _viewStateLiveData.value = ProgressBarState(false)
    }

    override fun setLiveDataOnDisconnected(sessionName: String) {
        _viewStateLiveData.value = OnDisconnected(sessionName)
    }

    override fun setLiveDataOnConnectionError(error: Throwable, sessionName: String) {
        _viewStateLiveData.value = OnWalletConnectConnectionError(error, sessionName)
    }

    override fun setLiveDataError(error: Throwable) {
        _errorLiveData.value = Event(error)
    }

    override fun updateWCState(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        _viewStateLiveData.postValue(UpdateOnSessionRequest(requestedNetwork, dialogType))
    }

    override fun closeScanner() {
        _viewStateLiveData.value = CloseScannerState
    }

    override fun handleSessionRequest(sessionRequest: OnSessionRequest) {
        val id = sessionRequest.chainId
        _viewStateLiveData.value = when {
            id == null -> {
                accountManager.getFirstActiveAccountOrNull(ChainId.ETH_MAIN)?.let { ethAccount -> account = ethAccount }
                OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.UNDEFINED_NETWORK_WARNING)
            }
            isNetworkNotSupported(chainId = id) -> {
                requestedNetwork = BaseNetworkData(id, String.empty)
                OnSessionRequest(
                    sessionRequest.meta,
                    requestedNetwork,
                    WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
                )
            }
            account.chainId != id -> getWalletConnectStateForNotEqualNetworks(sessionRequest, id)
            else -> {
                requestedNetwork = BaseNetworkData(id, getNetworkName(id))
                OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.NO_ALERT)
            }
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
        hideProgress()
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

    private fun getWalletConnectStateForNotEqualNetworks(sessionRequest: OnSessionRequest, chainId: Int): WalletConnectState {
        requestedNetwork = BaseNetworkData(chainId, getNetworkName(chainId))
        return accountManager.getFirstActiveAccountOrNull(chainId)?.let { newAccount ->
            account = newAccount
            OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.CHANGE_ACCOUNT_WARNING)
        }.orElse {
            OnSessionRequest(sessionRequest.meta, requestedNetwork, WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR)
        }
    }
}