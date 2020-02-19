package minerva.android.values

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.WalletConfig
import timber.log.Timber

class ValuesViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _balanceLiveData = MutableLiveData<HashMap<String, Balance>>()
    val balanceLiveData: LiveData<HashMap<String, Balance>> get() = _balanceLiveData

    private val _assetBalanceLiveData = MutableLiveData<Map<String, List<Asset>>>()
    val assetBalanceLiveData: LiveData<Map<String, List<Asset>>> get() = _assetBalanceLiveData

    fun refreshBalances() =
        launchDisposable {
            walletManager.refreshBalances()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        _balanceLiveData.value = it
                    },
                    onError = {
                        _errorLiveData.value = Event(it)
                        Timber.d("Refresh balance error: ${it.message}")
                    }
                )
        }

    fun getAssetBalance() =
        launchDisposable {
            walletManager.refreshAssetBalance()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { _assetBalanceLiveData.value = it },
                    onError = {
                        _errorLiveData.value = Event(it)
                        Timber.e("Refresh asset balance error: ${it.message}")
                    }
                )
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