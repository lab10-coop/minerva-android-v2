package minerva.android.values

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.WalletConfig
import timber.log.Timber
import java.math.BigDecimal

class ValuesViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData

    val balanceLiveData: LiveData<HashMap<String, BigDecimal>> = walletManager.balanceLiveData

    fun refreshBalances() {
        walletManager.refreshBalances()
    }

    fun removeValue(index: Int) {
        launchDisposable {
            walletManager.removeValue(index)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    //on complete is handled by LiveData
                    onError = {
                        Timber.e("Removing value with index $index failure")
                        _errorLiveData.value = Event(Throwable(it.message))
                    }
                )
        }
    }
}