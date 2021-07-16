package minerva.android.settings.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.storage.LocalStorage

class AuthenticationViewModel(private val localStorage: LocalStorage) : BaseViewModel() {

    var wasCredentialsChecked = false

    private val _protectKeysLiveData = MutableLiveData<Event<Boolean>>()
    val protectKeysLiveData: LiveData<Event<Boolean>> get() = _protectKeysLiveData

    private val _protectTransactionsLiveData = MutableLiveData<Event<Boolean>>()
    val protectTransactionsLiveData: LiveData<Event<Boolean>> get() = _protectTransactionsLiveData

    val isProtectKeysEnabled
        get() = localStorage.isProtectKeysEnabled

    fun toggleProtectKeys() {
        localStorage.apply {
            _protectKeysLiveData.value = Event(!isProtectKeysEnabled)
            isProtectKeysEnabled = !isProtectKeysEnabled
            _protectTransactionsLiveData.value = Event(isProtectTransactionsEnabled)
        }
    }

    fun toggleProtectTransactions() {
        localStorage.apply {
            _protectTransactionsLiveData.value = Event(!isProtectTransactionsEnabled)
            isProtectTransactionsEnabled = !isProtectTransactionsEnabled
        }
    }

    init {
        with(localStorage) {
            _protectKeysLiveData.value = Event(isProtectKeysEnabled)
            _protectTransactionsLiveData.value = Event(isProtectTransactionsEnabled)
        }
    }
}