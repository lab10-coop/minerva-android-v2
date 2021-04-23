package minerva.android.settings.fiat

import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Fiat
import minerva.android.walletmanager.storage.LocalStorage

class FiatViewModel(private val localStorage: LocalStorage, private val accountManager: AccountManager) : BaseViewModel() {

    fun getCurrentFiatPosition(): Int = Fiat.all.indexOf(localStorage.loadCurrentFiat()).let { currentPosition ->
        if (currentPosition == Int.InvalidIndex) FIRST_INDEX
        else currentPosition
    }

    fun saveCurrentFiat(fiat: String) {
        localStorage.saveCurrentFiat(fiat)
        accountManager.clearFiat()
    }

    companion object {
        private const val FIRST_INDEX = 0
    }
}