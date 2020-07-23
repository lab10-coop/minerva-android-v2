package minerva.android.onboarding.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.repository.seed.MasterSeedRepository

class CreateWalletViewModel(private val masterSeedRepository: MasterSeedRepository) : BaseViewModel() {

    private val _createWalletLiveData = MutableLiveData<Event<Unit>>()
    val createWalletLiveData: LiveData<Event<Unit>> get() = _createWalletLiveData

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun createMasterSeed() {
        launchDisposable {
            masterSeedRepository.createMasterSeed()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onComplete = {
                        masterSeedRepository.initWalletConfig()
                        _createWalletLiveData.value = Event(Unit)
                    },
                    onError = {
                        _errorLiveData.value = Event(it)
                        //Panic Button. Uncomment code below to save manually - not recommended
                        //_createWalletMutableLiveData.value = Event(Unit) //uncomment when offline app is needed
                    }
                )
        }
    }

}