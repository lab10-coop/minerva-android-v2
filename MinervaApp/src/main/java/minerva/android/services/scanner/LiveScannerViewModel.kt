package minerva.android.services.scanner

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

class LiveScannerViewModel(private val walletManager: WalletManager) : ViewModel() {

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
                onSuccess = {
                    if (it.isQrCodeValid) {
                        it.serviceName = getServiceName(token)
                        it.identityFields = getRequestedData(it.requestedData)
                        _scannerResultMutableLiveData.value = Event(it)
                    } else {
                        _scannerErrorMutableLiveData.value = Event(Throwable())
                    }
                },
                onError = {
                    _scannerErrorMutableLiveData.value = Event(it)
                }
            )
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
//    scanResult.substring(
//        FIRST_CHAR, scanResult.indexOf(
//            SEPARATOR
//        )
//    )

    fun onPause() {
        disposable?.dispose()
    }

    companion object {
        const val FIRST_CHAR = 0
        const val SEPARATOR = ":"
    }
}