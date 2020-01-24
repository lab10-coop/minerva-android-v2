package minerva.android.values.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.walletconfig.Network

class NewValueViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    private val _saveCompletedLiveData = MutableLiveData<Event<Int>>()
    val saveCompletedLiveData: LiveData<Event<Int>> get() = _saveCompletedLiveData

    private val _saveErrorLiveData = MutableLiveData<Event<Throwable>>()
    val saveErrorLiveData: LiveData<Event<Throwable>> get() = _saveErrorLiveData

    fun createNewValue(network: Network, position: Int) =
        walletManager.createValue(network, prepareName(network, position))
            .subscribeBy(
                onComplete = { _saveCompletedLiveData.value = Event(position) },
                onError = { _saveErrorLiveData.value = Event(Throwable("Unexpected creating value error: ${it.message}")) }
            )

    private fun prepareName(network: Network, position: Int) = String.format(VALUE_NAME_PATTERN, position, network.full)

    companion object {
        private const val VALUE_NAME_PATTERN = "#%d %s"
    }
}