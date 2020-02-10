package minerva.android.values

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.WalletConfig
import timber.log.Timber

class ValuesViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    var refreshBalanceDisposable: Disposable? = null

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _refreshBalancesErrorLiveData = MutableLiveData<Event<Throwable>>()
    val refreshBalancesErrorLiveData: LiveData<Event<Throwable>> get() = _refreshBalancesErrorLiveData

    private val _balanceLiveData = MutableLiveData<HashMap<String, Balance>>()
    val balanceLiveData: LiveData<HashMap<String, Balance>> get() = _balanceLiveData

    fun refreshBalances() {
        refreshBalanceDisposable = walletManager.refreshBalances()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { _balanceLiveData.value = it },
                onError = {
                    Timber.d("Refresh balance error: ${it.message}")
                    _refreshBalancesErrorLiveData.value = Event(it)
                }
            )
    }

    fun disposeRefreshBalance() {
        refreshBalanceDisposable?.dispose()
    }

    fun removeValue(index: Int) {
        launchDisposable {
            walletManager.removeValue(index)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    //on complete is handled by LiveData in WalletManager
                    onError = {
                        Timber.e("Removing value with index $index failure")
                        _errorLiveData.value = Event(Throwable(it.message))
                    }
                )
        }
    }
}