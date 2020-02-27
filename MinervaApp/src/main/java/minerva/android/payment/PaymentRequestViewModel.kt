package minerva.android.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.model.Payment

class PaymentRequestViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    lateinit var payment: Payment

    private val _decodeTokenMutableLiveData = MutableLiveData<Event<String?>>()
    val decodeTokenLiveData: LiveData<Event<String?>> get() = _decodeTokenMutableLiveData

    private val _errorMutableLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorMutableLiveData

    fun decodeJwtToken(token: String?) {
        token?.let {
            launchDisposable {
                walletManager.decodePaymentRequestToken(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = {
                            payment = it
                            _decodeTokenMutableLiveData.value = Event(it.serviceName) },
                        onError = { _errorMutableLiveData.value = Event(it) }
                    )
            }
        }.orElse {
            _errorMutableLiveData.value = Event(Throwable())
        }
    }
}