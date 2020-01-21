package minerva.android.values

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.Value
import timber.log.Timber

class ValueAddressViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    private val _loadValueLiveData = MutableLiveData<Event<Value>>()
    val loadValueLiveData: LiveData<Event<Value>> get() = _loadValueLiveData

    fun loadValue(position: Int) {
        computeKeys(walletManager.loadValue(position))
    }

    private fun computeKeys(loadedValue: Value) {
        loadedValue.let {
            walletManager.computeDerivedKey(loadedValue.index, callback = { error, privateKey, publicKey ->
                error?.let {
                    Timber.e("Computing keys error: $error")
                    return@computeDerivedKey
                }
                _loadValueLiveData.value = Event(Value(it.index, publicKey, privateKey, it.name, it.network, it.isDeleted))
            })
        }
    }
}