package minerva.android.settings.fiat

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Fiat
import minerva.android.walletmanager.storage.LocalStorage

class FiatViewModel(private val localStorage: LocalStorage, private val accountManager: AccountManager) : BaseViewModel() {

    //TODO klop add test
    fun getCurrentFiatPosition(): Int {
        Fiat.all.forEachIndexed { index, fiat ->
            if (fiat == localStorage.loadCurrentFiat()) return index
        }
        return FIRST_INDEX
    }

    //TODO klop add test
    fun saveCurrentFiat(fiat: String) {
        localStorage.saveCurrentFiat(fiat)
        accountManager.clearFiat()
    }

    companion object {
        private const val FIRST_INDEX = 0
    }
}