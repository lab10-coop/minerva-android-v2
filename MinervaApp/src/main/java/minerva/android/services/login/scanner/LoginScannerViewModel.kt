package minerva.android.services.login.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginUtils.getLoginStatus
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.CredentialQrResponse
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.model.ServiceQrResponse
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.defs.ServiceType
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.mappers.CredentialQrCodeResponseMapper
import minerva.android.walletmanager.walletActions.WalletActionsRepository

class LoginScannerViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository
) : BaseViewModel() {

    private val _scannerResultLiveData = MutableLiveData<Event<ServiceQrResponse>>()
    val scannerResultLiveData: LiveData<Event<ServiceQrResponse>> get() = _scannerResultLiveData

    private val _scannerErrorLiveData = MutableLiveData<Event<Throwable>>()
    val scannerErrorLiveData: LiveData<Event<Throwable>> get() = _scannerErrorLiveData

    private val _knownUserLoginLiveData = MutableLiveData<Event<LoginPayload>>()
    val knownUserLoginLiveData: LiveData<Event<LoginPayload>> get() = _knownUserLoginLiveData

    private val _handleBindCredentialSuccessLiveData = MutableLiveData<Event<String>>()
    val handleBindCredentialSuccessLiveData: LiveData<Event<String>> get() = _handleBindCredentialSuccessLiveData

    private val _handleBindCredentialErrorLiveData = MutableLiveData<Event<Throwable>>()
    val handleBindCredentialErrorLiveData: LiveData<Event<Throwable>> get() = _handleBindCredentialErrorLiveData

    fun validateResult(token: String) {
        launchDisposable {
            serviceManager.decodeQrCodeResponse(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { handleQrCodeResponse(it) },
                    onError = { _scannerErrorLiveData.value = Event(it) }
                )
        }
    }

    private fun handleQrCodeResponse(response: QrCodeResponse) {
        when (response) {
            is ServiceQrResponse -> handleServiceQrCodeResponse(response)
            is CredentialQrResponse -> handleCredentialQrCodeResponse(response)
        }
    }

    private fun handleCredentialQrCodeResponse(response: CredentialQrResponse) {
        if (response.loggedInDid.isNotEmpty()) {
            launchDisposable {
                serviceManager.bindCredentialToIdentity(CredentialQrCodeResponseMapper.map(response))
                    .observeOn(Schedulers.io())
                    .flatMap { walletActionsRepository.saveWalletActions(getWalletAction(response)).toSingleDefault(it) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { _handleBindCredentialSuccessLiveData.value = Event(it) },
                        onError = { _handleBindCredentialErrorLiveData.value = Event(it) }
                    )
            }
        } else {
            _handleBindCredentialErrorLiveData.value = Event(NoBindedCredentialThrowable())
        }
    }

    private fun getWalletAction(response: CredentialQrResponse): WalletAction =
        WalletAction(
            WalletActionType.CREDENTIAL, WalletActionStatus.ADDED,
            response.lastUsed,
            hashMapOf(WalletActionFields.CREDENTIAL_NAME to response.name)
        )

    private fun handleServiceQrCodeResponse(response: ServiceQrResponse) {
        if (isLoggedInToChargingCarStation(response)) _knownUserLoginLiveData.value = Event(getLoginPayload(response))
        else _scannerResultLiveData.value = Event(response)
    }

    private fun isLoggedInToChargingCarStation(response: QrCodeResponse) =
        response.issuer == ServiceType.CHARGING_STATION && serviceManager.isAlreadyLoggedIn(response.issuer)

    private fun getLoginPayload(response: ServiceQrResponse) =
        LoginPayload(getLoginStatus(response), serviceManager.getLoggedInIdentityPublicKey(response.issuer), response)
}