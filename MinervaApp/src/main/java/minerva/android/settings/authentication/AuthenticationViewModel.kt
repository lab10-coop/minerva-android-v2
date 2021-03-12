package minerva.android.settings.authentication

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.storage.LocalStorage

class AuthenticationViewModel(private val localStorage: LocalStorage) : BaseViewModel() {

    fun isAuthenticationEnabled() = localStorage.isAuthenticationEnabled

    fun toggleAuthentication() {
        localStorage.isAuthenticationEnabled = !isAuthenticationEnabled()
    }
}