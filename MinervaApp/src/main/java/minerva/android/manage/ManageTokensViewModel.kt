package minerva.android.manage

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.AssetVisibilitySettings
import minerva.android.walletmanager.model.Token
import minerva.android.walletmanager.storage.LocalStorage

class ManageTokensViewModel(
    private val accountManager: AccountManager,
    private val localStorage: LocalStorage,
    private val tokenManager: TokenManager
) : BaseViewModel() {

    lateinit var account: Account
    private lateinit var assetVisibilitySettings: AssetVisibilitySettings

    fun initViewModel(index: Int) {
        account = accountManager.loadAccount(index)
        assetVisibilitySettings = localStorage.getAssetVisibilitySettings()
    }

    fun loadTokens() = account.network.let {
        listOf(Token(it.full, it.short)) + tokenManager.loadTokens(it.short)
    }

    fun getTokenVisibilitySettings(assetAddress: String): Boolean =
        assetVisibilitySettings.getAssetVisibility(account.address, assetAddress) ?: false


    fun saveTokenVisibilitySettings(assetAddress: String, visibility: Boolean) {
        assetVisibilitySettings = localStorage.saveAssetVisibilitySettings(
            assetVisibilitySettings.updateAssetVisibility(account.address, assetAddress, visibility)
        )
    }
}