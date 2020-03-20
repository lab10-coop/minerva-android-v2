package minerva.android.services.login.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.services.login.identity.ChooseIdentityViewModel
import minerva.android.services.login.uitls.LoginPayload
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_QUICK_USER
import minerva.android.services.login.uitls.LoginStatus.Companion.KNOWN_USER
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.model.QrCodeResponse
import minerva.android.walletmanager.storage.ServiceName
import minerva.android.walletmanager.storage.ServiceType

class ScannerViewModel(private val walletManager: WalletManager) : ViewModel() {

    private var disposable: Disposable? = null

    private val _scannerResultMutableLiveData = MutableLiveData<Event<QrCodeResponse>>()
    val scannerResultLiveData: LiveData<Event<QrCodeResponse>> get() = _scannerResultMutableLiveData

    private val _scannerErrorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val scannerErrorLiveData: LiveData<Event<Throwable>> get() = _scannerErrorMutableLiveData

    private val _knownUserLoginMutableLiveData = MutableLiveData<Event<LoginPayload>>()
    val knownUserLoginMutableLiveData: LiveData<Event<LoginPayload>> get() = _knownUserLoginMutableLiveData

    fun validateResult(token: String) {
        disposable = walletManager.decodeQrCodeResponse(token)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { handleQrCodeResponse(it) },
                onError = { _scannerErrorMutableLiveData.value = Event(it) }
            )
    }

    private fun handleQrCodeResponse(response: QrCodeResponse) {
        response.run {
            serviceName = getServiceName(response)
            identityFields = getRequestedData(requestedData)
        }
        if (walletManager.isAlreadyLoggedIn(response.issuer)) {
            _knownUserLoginMutableLiveData.value =
                Event(LoginPayload(getLoginStatus(response), walletManager.getLoggedInIdentityPublicKey(response.issuer), response))
        } else {
            _scannerResultMutableLiveData.value = Event(response)
        }
    }

    private fun getLoginStatus(qrCodeResponse: QrCodeResponse): Int =
        if (qrCodeResponse.requestedData.contains(ChooseIdentityViewModel.FCM_ID)) KNOWN_QUICK_USER
        else KNOWN_USER

    private fun getRequestedData(requestedData: ArrayList<String>): String {
        var identityFields: String = String.Empty
        requestedData.forEach { identityFields += "$it " }
        return identityFields
    }

    private fun getServiceName(response: QrCodeResponse): String =
        when (response.issuer) {
            ServiceType.UNICORN_LOGIN -> ServiceName.UNICORN_LOGIN_NAME
            ServiceType.CHARGING_STATION -> ServiceName.CHARGING_STATION_NAME
            else -> String.Empty
        }

    fun onPause() {
        disposable?.dispose()
    }
}