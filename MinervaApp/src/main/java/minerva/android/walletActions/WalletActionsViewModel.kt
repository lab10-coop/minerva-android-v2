package minerva.android.walletActions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.WalletActionClustered
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import timber.log.Timber

class WalletActionsViewModel(private val walletActionsRepository: WalletActionsRepository) : BaseViewModel() {

    private val _errorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData: LiveData<Event<Throwable>> get() = _errorLiveData

    private val _walletActionsLiveData = MutableLiveData<Event<List<WalletActionClustered>>>()
    val walletActionsLiveData: LiveData<Event<List<WalletActionClustered>>> get() = _walletActionsLiveData

    private val _loadingLiveData = MutableLiveData<Event<Boolean>>()
    val loadingLiveData: LiveData<Event<Boolean>> get() = _loadingLiveData

    fun fetchWalletActions() {
        _loadingLiveData.value = Event(true)
        launchDisposable {
            walletActionsRepository.getWalletActions()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loadingLiveData.value = Event(false) }
                .subscribeBy(
                    onNext = { _walletActionsLiveData.value = Event(it) },
                    onError = {
                        _errorLiveData.value = Event(it)
                        Timber.d("Fetch wallet actions error: ${it.message}")
                    }
                )
        }
    }

}