package minerva.android.token.ramp

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager

class RampViewModel(private val accountManager: AccountManager) : BaseViewModel() {


    //TODO klop test
    fun getValidAccounts(chainId: Int) = accountManager.getAllAccounts(chainId)
}