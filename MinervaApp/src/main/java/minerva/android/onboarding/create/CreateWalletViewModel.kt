package minerva.android.onboarding.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import timber.log.Timber

class CreateWalletViewModel(private val masterSeedRepository: MasterSeedRepository) : BaseViewModel() {

    private val _createWalletLiveData = MutableLiveData<Event<Unit>>()
    val createWalletLiveData: LiveData<Event<Unit>> get() = _createWalletLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun createWalletConfig() {
        launchDisposable {
            masterSeedRepository.createWalletConfig()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(true) }
                .doOnEvent { _loadingLiveData.value = Event(false) }
                .doOnTerminate { _createWalletLiveData.value = Event(Unit) }
                .subscribeBy(onError = { Timber.e("Create wallet error: $it") })
        }
    }
}