package minerva.android.identities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletConfig

class IdentitiesViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    fun removeIdentity(identity: Identity) {
        launchDisposable {
            walletManager.removeIdentity(identity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { /* Result is handled by LiveData in WalletManager */ },
                    onError = { _errorLiveData.value = Event(it) }
                )
        }
    }
}