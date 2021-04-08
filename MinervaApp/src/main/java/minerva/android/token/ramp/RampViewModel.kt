package minerva.android.token.ramp

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account

class RampViewModel(private val accountManager: AccountManager) : BaseViewModel() {

    private var currentAccounts: List<Account> = listOf()
    var spinnerPosition = 0

    //TODO klop test
    fun getValidAccounts(chainId: Int) = accountManager.getAllActiveAccounts(chainId).apply {
        currentAccounts = this
    }

    //TODO klop test
    fun getCurrentAccount() = currentAccounts[spinnerPosition]
}