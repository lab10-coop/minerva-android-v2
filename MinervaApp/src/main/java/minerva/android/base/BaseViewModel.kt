package minerva.android.base

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.walletmanager.model.WalletAction
import timber.log.Timber

abstract class BaseViewModel : ViewModel() {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    open fun onResume() {}

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
        unsubscribe()
    }

    fun launchDisposable(job: () -> Disposable) {
        compositeDisposable.add(job())
    }

    open fun onDestroy() {
        compositeDisposable.dispose()
    }

    open fun unsubscribe() {}
}