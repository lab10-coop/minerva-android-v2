package minerva.android.onboarding.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager

class CreateWalletViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    private val _createWalletMutableLiveData = MutableLiveData<Event<Unit>>()
    val createWalletLiveData: LiveData<Event<Unit>> get() = _createWalletMutableLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun createMasterSeed() {
        launchDisposable {
            walletManager.createMasterSeed()
                .flatMapCompletable { walletManager.createWalletConfig(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = { _createWalletMutableLiveData.value = Event(Unit) },
                    onError = {
                        _errorLiveData.value = Event(it)
                        //TODO Panic Button. Uncomment code below to save manually - not recommended
                        //_createWalletMutableLiveData.value = Event(Unit) //uncomment when offline app is needed
                    }
                )
        }
    }

}