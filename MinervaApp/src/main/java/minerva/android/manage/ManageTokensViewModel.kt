package minerva.android.manage

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.TokenVisibilitySettings
import minerva.android.walletmanager.model.Token
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.widget.repository.getMainTokenIconRes

class ManageTokensViewModel(
    private val accountManager: AccountManager,
    private val localStorage: LocalStorage,
    private val tokenManager: TokenManager
) : BaseViewModel() {

    lateinit var account: Account
    private lateinit var tokenVisibilitySettings: TokenVisibilitySettings

    fun initViewModel(index: Int) {
        account = accountManager.loadAccount(index)
        tokenVisibilitySettings = localStorage.getTokenVisibilitySettings()
    }

    fun loadTokens() = account.network.let {
        listOf(Token(it.token, it.short, logoRes = getMainTokenIconRes(it.short))) + tokenManager.loadTokens(it.short)
    }

    fun getTokenVisibilitySettings(assetAddress: String): Boolean =
        tokenVisibilitySettings.getAssetVisibility(account.address, assetAddress) ?: false


    fun saveTokenVisibilitySettings(assetAddress: String, visibility: Boolean) {
        tokenVisibilitySettings = localStorage.saveTokenVisibilitySettings(
            tokenVisibilitySettings.updateTokenVisibility(account.address, assetAddress, visibility)
        )
    }
}