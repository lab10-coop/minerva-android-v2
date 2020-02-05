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
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.QrCodeResponse

class ScannerViewModel(private val walletManager: WalletManager) : ViewModel() {

    private var disposable: Disposable? = null

    private val _scannerResultMutableLiveData = MutableLiveData<Event<QrCodeResponse>>()
    val scannerResultLiveData: LiveData<Event<QrCodeResponse>> get() = _scannerResultMutableLiveData

    private val _scannerErrorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val scannerErrorLiveData: LiveData<Event<Throwable>> get() = _scannerErrorMutableLiveData

    fun validateResult(token: String) {
        disposable = walletManager.decodeJwtToken(token)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { handleQrCodeResponse(it, token) },
                onError = { _scannerErrorMutableLiveData.value = Event(it) }
            )
    }

    private fun handleQrCodeResponse(response: QrCodeResponse, token: String) {
        response.run {
            serviceName = getServiceName(token)
            identityFields = getRequestedData(requestedData)
            _scannerResultMutableLiveData.value = Event(this)
        }
    }

    private fun getRequestedData(requestedData: ArrayList<String>): String {
        var identityFields: String = String.Empty
        requestedData.forEach {
            identityFields += "$it "
        }
        return identityFields
    }

    //    TODO implement getting service name for QR code, leave it for demo purposes
    private fun getServiceName(scanResult: String) = "Minerva Service"

    fun onPause() {
        disposable?.dispose()
    }
}