package minerva.android.services.login.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginUtils.getLoginStatus
import minerva.android.walletmanager.exception.NoBindedCredentialThrowable
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.ServiceType
import minerva.android.walletmanager.model.defs.WalletActionFields
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.ADDED
import minerva.android.walletmanager.model.defs.WalletActionStatus.Companion.FAILED
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.mappers.CredentialQrCodeResponseMapper
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class LoginScannerViewModel(
    private val serviceManager: ServiceManager,
    private val walletActionsRepository: WalletActionsRepository,
    private val identityManager: IdentityManager
) : BaseViewModel() {

    private val _handleServiceQrCodeLiveData = MutableLiveData<Event<ServiceQrCode>>()
    val handleServiceQrCodeLiveData: LiveData<Event<ServiceQrCode>> get() = _handleServiceQrCodeLiveData

    private val _scannerErrorLiveData = MutableLiveData<Event<Throwable>>()
    val scannerErrorLiveData: LiveData<Event<Throwable>> get() = _scannerErrorLiveData

    private val _knownUserLoginLiveData = MutableLiveData<Event<LoginPayload>>()
    val knownUserLoginLiveData: LiveData<Event<LoginPayload>> get() = _knownUserLoginLiveData

    private val _bindCredentialSuccessLiveData = MutableLiveData<Event<String>>()
    val bindCredentialSuccessLiveData: LiveData<Event<String>> get() = _bindCredentialSuccessLiveData

    private val _bindCredentialErrorLiveData = MutableLiveData<Event<Throwable>>()
    val bindCredentialErrorLiveData: LiveData<Event<Throwable>> get() = _bindCredentialErrorLiveData

    private val _updateBindedCredential = MutableLiveData<Event<CredentialQrCode>>()
    val updateBindedCredential: LiveData<Event<CredentialQrCode>> get() = _updateBindedCredential

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

    private fun handleQrCodeResponse(qrCode: QrCode) {
        when (qrCode) {
            is ServiceQrCode -> handleServiceQrCodeResponse(qrCode)
            is CredentialQrCode -> handleCredentialQrCodeResponse(qrCode)
        }
    }

    private fun handleCredentialQrCodeResponse(credentialQrCode: CredentialQrCode) {
        val credential = CredentialQrCodeResponseMapper.map(credentialQrCode)
        if (identityManager.isCredentialLoggedIn(credential)) {
            _updateBindedCredential.value = Event(credentialQrCode)
        } else {
            launchDisposable {
                identityManager.bindCredentialToIdentity(credential)
                    .onErrorResumeNext { SingleSource { saveWalletAction(getWalletAction(credential, FAILED)) } }
                    .doOnSuccess { saveWalletAction(getWalletAction(credential, ADDED)) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { _bindCredentialSuccessLiveData.value = Event(it) },
                        onError = { _bindCredentialErrorLiveData.value = Event(it) }
                    )
            }
        }
    }

    private fun saveWalletAction(walletAction: WalletAction) {
        launchDisposable {
            walletActionsRepository.saveWalletActions(walletAction)
                .toSingleDefault(walletAction.status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        if (it == FAILED)
                            _bindCredentialErrorLiveData.value = Event(NoBindedCredentialThrowable())
                        Timber.d("Save bind credential wallet action success")
                    },
                    onError = { Timber.e("Save bind credential error: $it") }
                )
        }
    }

    private fun getWalletAction(credential: Credential, status: Int): WalletAction =
        WalletAction(
            WalletActionType.CREDENTIAL, status,
            credential.lastUsed,
            hashMapOf(WalletActionFields.CREDENTIAL_NAME to credential.name)
        )

    private fun handleServiceQrCodeResponse(serviceQrCode: ServiceQrCode) {
        if (isLoggedInToChargingCarStation(serviceQrCode)) _knownUserLoginLiveData.value = Event(getLoginPayload(serviceQrCode))
        else _handleServiceQrCodeLiveData.value = Event(serviceQrCode)
    }

    private fun isLoggedInToChargingCarStation(qrCode: ServiceQrCode) =
        qrCode.issuer == ServiceType.CHARGING_STATION && serviceManager.isAlreadyLoggedIn(qrCode.issuer)

    private fun getLoginPayload(qrCode: ServiceQrCode) =
        LoginPayload(getLoginStatus(qrCode), serviceManager.getLoggedInIdentityPublicKey(qrCode.issuer), qrCode)
}