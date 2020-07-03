package minerva.android.accounts.address

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Account

class AccountAddressViewModel(private val accountManager: AccountManager) : BaseViewModel() {

    private val _loadAccountLiveData = MutableLiveData<Event<Account>>()
    val loadAccountLiveData: LiveData<Event<Account>> get() = _loadAccountLiveData

    fun loadAccount(position: Int) {
        _loadAccountLiveData.value = Event(accountManager.loadAccount(position))
    }
}