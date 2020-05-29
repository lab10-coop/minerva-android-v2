package minerva.android.base

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseViewModel : ViewModel() {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    open fun onResume() {}

    fun onPause() {
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