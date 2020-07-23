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
import minerva.android.services.login.uitls.LoginUtils.getRequestedData
import minerva.android.services.login.uitls.LoginUtils.getServiceName
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.QrCodeResponse

class LoginScannerViewModel(private val serviceManager: ServiceManager) : BaseViewModel() {

    private val _scannerResultLiveData = MutableLiveData<Event<QrCodeResponse>>()
    val scannerResultLiveData: LiveData<Event<QrCodeResponse>> get() = _scannerResultLiveData

    private val _scannerErrorLiveData = MutableLiveData<Event<Throwable>>()
    val scannerErrorLiveData: LiveData<Event<Throwable>> get() = _scannerErrorLiveData

    private val _knownUserLoginLiveData = MutableLiveData<Event<LoginPayload>>()
    val knownUserLoginLiveData: LiveData<Event<LoginPayload>> get() = _knownUserLoginLiveData

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
        response.run {
            serviceName = getServiceName(response)
            identityFields = getRequestedData(requestedData)
        }
        checkAlreadyLoginUser(response)
    }

    private fun checkAlreadyLoginUser(response: QrCodeResponse) {
        if (serviceManager.isAlreadyLoggedIn(response.issuer)) {
            _knownUserLoginLiveData.value =
                Event(LoginPayload(getLoginStatus(response), serviceManager.getLoggedInIdentityPublicKey(response.issuer), response))
        } else {
            _scannerResultLiveData.value = Event(response)
        }
    }
}