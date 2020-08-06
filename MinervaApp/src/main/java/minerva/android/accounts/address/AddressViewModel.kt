package minerva.android.accounts.address

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.exception.NoAddressPageFragment
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.identity.IdentityManager
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.wrapped.WrappedFragmentType

class AddressViewModel(private val identityManager: IdentityManager, private val accountManager: AccountManager) : BaseViewModel() {

    private val _loadMinervaPrimitiveLiveData = MutableLiveData<Event<MinervaPrimitive>>()
    val loadMinervaPrimitiveLiveData: LiveData<Event<MinervaPrimitive>> get() = _loadMinervaPrimitiveLiveData

    fun loadMinervaPrimitive(fragmentType: WrappedFragmentType, position: Int) {
        _loadMinervaPrimitiveLiveData.value = when (fragmentType) {
            WrappedFragmentType.IDENTITY_ADDRESS -> Event(identityManager.loadIdentity(position))
            WrappedFragmentType.ACCOUNT_ADDRESS -> Event(accountManager.loadAccount(position))
            else -> throw NoAddressPageFragment()
        }
    }
}