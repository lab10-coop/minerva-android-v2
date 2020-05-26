package minerva.android.values.address

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.model.Value

class ValueAddressViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    private val _loadValueLiveData = MutableLiveData<Event<Value>>()
    val loadValueLiveData: LiveData<Event<Value>> get() = _loadValueLiveData

    fun loadValue(position: Int) {
        _loadValueLiveData.value = Event(walletManager.loadValue(position))
    }
}