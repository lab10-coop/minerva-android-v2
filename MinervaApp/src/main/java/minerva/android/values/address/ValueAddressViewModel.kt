package minerva.android.values.address

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.values.ValueManager
import minerva.android.walletmanager.model.Value

class ValueAddressViewModel(private val valueManager: ValueManager) : BaseViewModel() {

    private val _loadValueLiveData = MutableLiveData<Event<Value>>()
    val loadValueLiveData: LiveData<Event<Value>> get() = _loadValueLiveData

    fun loadValue(position: Int) {
        _loadValueLiveData.value = Event(valueManager.loadValue(position))
    }
}