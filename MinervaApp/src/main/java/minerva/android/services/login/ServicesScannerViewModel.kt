package minerva.android.services.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.walletconnect.BaseWalletConnectScannerViewModel
import minerva.android.accounts.walletconnect.WalletConnectAlertType
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.CredentialQrCode
import minerva.android.walletmanager.model.QrCode
import minerva.android.walletmanager.model.ServiceQrCode
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.wallet.WalletAction
import minerva.android.walletmanager.model.walletconnect.BaseNetworkData
import minerva.android.walletmanager.model.walletconnect.WalletConnectPeerMeta
import minerva.android.walletmanager.model.walletconnect.WalletConnectUriUtils
import minerva.android.walletmanager.provider.UnsupportedNetworkRepository
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequest
import minerva.android.walletmanager.repository.walletconnect.OnSessionRequestV2
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class ServicesScannerViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val walletConnectRepository: WalletConnectRepository,
    private val accountManager: AccountManager,
    logger: Logger,
    private val identityManager: IdentityManager,
    private val unsupportedNetworkRepository: UnsupportedNetworkRepository
) : BaseWalletConnectScannerViewModel(
    accountManager,
    walletConnectRepository,
    logger,
    walletActionsRepository,
    unsupportedNetworkRepository
) {

    private val _viewStateLiveData = MutableLiveData<ServicesScannerViewState>()
    val viewStateLiveData: LiveData<ServicesScannerViewState> get() = _viewStateLiveData

    override var account: Account = Account(Int.InvalidId)

    override fun hideProgress() {
        _viewStateLiveData.value = ProgressBarState(false)
    }

    override fun setLiveDataOnDisconnected(sessionName: String) {
        _viewStateLiveData.value = WalletConnectDisconnectResult(sessionName)
    }

    override fun setLiveDataOnConnectionError(error: Throwable, sessionName: String) {
        _viewStateLiveData.value = WalletConnectConnectionError(error, sessionName)
    }

    override fun setLiveDataError(error: Throwable) {
        _viewStateLiveData.value = Error(error)
    }

    override fun updateWCState(network: BaseNetworkData, dialogType: WalletConnectAlertType) {
        _viewStateLiveData.postValue(WalletConnectUpdateDataState(network, dialogType))
    }

    override fun closeScanner(isMobileWalletConnect: Boolean) {
        _viewStateLiveData.value = CloseScannerState
    }

    fun validateResult(result: String) {
        if (WalletConnectUriUtils.isValidWalletConnectUri(result)) {
            handleWalletConnectQrCodeResponse(result)
            return
        }
        // todo: validate jwt before decoding it?
        launchDisposable {
            serviceManager.decodeJwtToken(result)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { handleLoginQrCodeResponse(it) },
                    onError = { error ->
                        Timber.e(error)
                        setLiveDataError(error)
                    }
                )
        }
    }

    private fun handleLoginQrCodeResponse(qrCode: QrCode) {
        when (qrCode) {
            is ServiceQrCode -> _viewStateLiveData.value = ServiceLoginResult(qrCode)
            is CredentialQrCode -> handleCredentialQrCodeResponse(qrCode)
        }
    }

    private fun handleCredentialQrCodeResponse(qrCode: CredentialQrCode) {
        if (identityManager.isCredentialLoggedIn(qrCode)) {
            _viewStateLiveData.value = UpdateCredentialsLoginResult(qrCode)
        } else {
            launchDisposable {
                identityManager.bindCredentialToIdentity(qrCode)
                    .onErrorResumeNext { error ->
                        SingleSource {
                            saveWalletAction(
                                getWalletCredentialAction(qrCode.lastUsed, qrCode.name, WalletActionStatus.FAILED),
                                error
                            )
                        }
                    }
                    .doOnSuccess {
                        saveWalletAction(
                            getWalletCredentialAction(
                                qrCode.lastUsed,
                                qrCode.name,
                                WalletActionStatus.ADDED
                            )
                        )
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { _viewStateLiveData.value = CredentialsLoginResult(it) },
                        onError = { setLiveDataError(it) }
                    )
            }
        }
    }

    private fun handleWalletConnectQrCodeResponse(qrCode: String) {
        _viewStateLiveData.value = CorrectWalletConnectResult
        currentSession = walletConnectRepository.getWCSessionFromQr(qrCode)
        walletConnectRepository.connect(currentSession)
    }

    // todo: why is this duplicate with WalletConnectInteracionsViewModel??
    override fun handleSessionRequest(sessionRequest: OnSessionRequest) {
        //if ethereum was chosen set unknown id for showing all networks
        val id: Int? = if (ChainId.ETH_MAIN == sessionRequest.chainId) null else sessionRequest.chainId
        when {
            id == null -> {
                accountManager.getFirstActiveAccountOrNull(ChainId.ETH_MAIN)?.let { ethAccount -> account = ethAccount }
                _viewStateLiveData.value = WalletConnectSessionRequestResult(
                    sessionRequest.meta,
                    requestedNetwork,
                    WalletConnectAlertType.UNDEFINED_NETWORK_WARNING
                )
            }
            isNetworkNotSupported(chainId = id) -> {
                requestedNetwork = BaseNetworkData(id, String.Empty)
                _viewStateLiveData.value = WalletConnectSessionRequestResult(
                    sessionRequest.meta,
                    requestedNetwork,
                    WalletConnectAlertType.UNSUPPORTED_NETWORK_WARNING
                )
                fetchUnsupportedNetworkName(id)
            }
            else ->  _viewStateLiveData.value = getWalletConnectStateForRequestedNetwork(sessionRequest, id)
        }
    }

    // todo: why is this duplicate with WalletConnectInteracionsViewModel??
    // todo: implement
    override fun handleSessionRequestV2(sessionRequest: OnSessionRequestV2) {
        _viewStateLiveData.value = WalletConnectSessionRequestResultV2(
            WalletConnectPeerMeta(
                // todo
            ),
            // todo: list
            emptyList()
            /*
            WalletConnectRepositoryImpl.proposalNamespacesToChainNames(
                sessionRequest.sessionProposal.requiredNamespaces
            )
            */
        )

    }

    private fun getWalletConnectStateForRequestedNetwork(sessionRequest: OnSessionRequest, chainId: Int): ServicesScannerViewState {
        requestedNetwork = BaseNetworkData(chainId, getNetworkName(chainId))
        return accountManager.getFirstActiveAccountOrNull(chainId)?.let { newAccount ->
            account = newAccount
            WalletConnectSessionRequestResult(
                sessionRequest.meta,
                requestedNetwork,
                WalletConnectAlertType.CHANGE_ACCOUNT_WARNING
            )
        }.orElse {
            WalletConnectSessionRequestResult(
                sessionRequest.meta,
                requestedNetwork,
                WalletConnectAlertType.NO_AVAILABLE_ACCOUNT_ERROR
            )
        }
    }

    private fun saveWalletAction(walletAction: WalletAction, error: Throwable? = null) {
        launchDisposable {
            walletActionsRepository.saveWalletActions(listOf(walletAction))
                .toSingleDefault(Pair(walletAction.status, error))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { (status, error) ->
                        Timber.d("Save bind credential wallet action success")
                        handleSavingWalletAction(status, error)
                    },
                    onError = { Timber.e("Save bind credential error: $it") }
                )
        }
    }

    private fun handleSavingWalletAction(status: Int, error: Throwable?) {
        when {
            status == WalletActionStatus.FAILED && error is AutomaticBackupFailedThrowable ->
                _viewStateLiveData.value = Error(AutomaticBackupFailedThrowable())
            status == WalletActionStatus.FAILED -> _viewStateLiveData.value = Error(NoBindedCredentialThrowable())
        }
    }

    private fun getWalletCredentialAction(lastUsed: Long, name: String, status: Int): WalletAction =
        WalletAction(
            WalletActionType.CREDENTIAL, status,
            lastUsed,
            hashMapOf(WalletActionFields.CREDENTIAL_NAME to name)
        )
}