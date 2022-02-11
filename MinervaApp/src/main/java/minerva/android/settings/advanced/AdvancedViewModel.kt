package minerva.android.settings.advanced

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import timber.log.Timber

class AdvancedViewModel(private val  walletConfigManager: WalletConfigManager) : BaseViewModel() {

    private val _resetTokensLiveData = MutableLiveData<Event<Result<Any>>>()
    val resetTokensLiveData: LiveData<Event<Result<Any>>> = _resetTokensLiveData

    fun resetTokens() {
        launchDisposable {
            walletConfigManager.removeAllTokens()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { _resetTokensLiveData.value = Event(Result.success(Unit)) },
                    onError = {
                        Timber.e(it)
                        _resetTokensLiveData.value = Event(Result.failure(it))
                    }
                )
        }

    }
}