package minerva.android.token

import androidx.annotation.VisibleForTesting
import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.token.NativeToken
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.widget.repository.getMainTokenIconRes

class ManageTokensViewModel(
    private val accountManager: AccountManager,
    private val localStorage: LocalStorage,
    private val tokenManager: TokenManager
) : BaseViewModel() {

    lateinit var account: Account
    @VisibleForTesting
    lateinit var tokenVisibilitySettings: TokenVisibilitySettings

    fun initViewModel(index: Int) {
        account = accountManager.loadAccount(index)
        tokenVisibilitySettings = localStorage.getTokenVisibilitySettings()
    }

    fun loadTokens() = account.network.let {
        listOf(NativeToken(it.chainId, it.name, it.token, logoRes = getMainTokenIconRes(it.chainId))) + tokenManager.loadCurrentTokens(it.chainId)
    }

    fun getTokenVisibilitySettings(assetAddress: String): Boolean =
        tokenVisibilitySettings.getTokenVisibility(account.address, assetAddress) ?: false


    fun saveTokenVisibilitySettings(assetAddress: String, visibility: Boolean) {
        tokenVisibilitySettings = localStorage.saveTokenVisibilitySettings(
            tokenVisibilitySettings.updateTokenVisibility(account.address, assetAddress, visibility)
        )
    }
}