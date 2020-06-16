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

    private val _scannerResultMutableLiveData = MutableLiveData<Event<QrCodeResponse>>()
    val scannerResultLiveData: LiveData<Event<QrCodeResponse>> get() = _scannerResultMutableLiveData

    private val _scannerErrorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val scannerErrorLiveData: LiveData<Event<Throwable>> get() = _scannerErrorMutableLiveData

    private val _knownUserLoginMutableLiveData = MutableLiveData<Event<LoginPayload>>()
    val knownUserLoginMutableLiveData: LiveData<Event<LoginPayload>> get() = _knownUserLoginMutableLiveData

    fun validateResult(token: String) {
        launchDisposable {
            serviceManager.decodeQrCodeResponse(token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { handleQrCodeResponse(it) },
                    onError = { _scannerErrorMutableLiveData.value = Event(it) }
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
            _knownUserLoginMutableLiveData.value =
                Event(LoginPayload(getLoginStatus(response), serviceManager.getLoggedInIdentityPublicKey(response.issuer), response))
        } else {
            _scannerResultMutableLiveData.value = Event(response)
        }
    }
}