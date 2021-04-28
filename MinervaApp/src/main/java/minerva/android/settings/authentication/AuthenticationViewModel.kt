package minerva.android.settings.authentication

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.storage.LocalStorage

class AuthenticationViewModel(private val localStorage: LocalStorage) : BaseViewModel() {

    var wasCredentialsChecked = false

    val isProtectKeysEnabled
        get() = localStorage.isProtectKeysEnabled

    val isProtectTransactionsEnabled
        get() = localStorage.isProtectTransactionsEnabled

    fun toggleProtectKeys() {
        localStorage.isProtectKeysEnabled = !isProtectKeysEnabled
    }

    fun toggleProtectTransactions() {
        localStorage.isProtectTransactionsEnabled = !isProtectTransactionsEnabled
    }
}