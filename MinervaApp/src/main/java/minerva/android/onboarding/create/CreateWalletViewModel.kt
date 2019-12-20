package minerva.android.onboarding.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.MasterKey

class CreateWalletViewModel(private val walletManager: WalletManager) : ViewModel() {

    private var disposable: Disposable? = null

    private val _createWalletMutableLiveData = MutableLiveData<Event<Unit>>()
    val createWalletLiveData: LiveData<Event<Unit>> get() = _createWalletMutableLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun createMasterSeed() {
        walletManager.createMasterKeys { error, privateKey, publicKey ->
            if (error == null) {
                disposable = walletManager.createDefaultWalletConfig(MasterKey(publicKey, privateKey))
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe { _loadingLiveData.value = Event(true) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnEvent { _loadingLiveData.value = Event(false) }
                    .subscribeBy(
                        onComplete = {
                            _createWalletMutableLiveData.value = Event(Unit)
                        },
                        onError = {
                            _errorLiveData.value = Event(it)
                        }
                    )
            }
        }
    }

    fun onPause() {
        disposable?.dispose()
    }
}