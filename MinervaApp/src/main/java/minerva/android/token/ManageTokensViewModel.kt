package minerva.android.token

import androidx.annotation.VisibleForTesting
import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.NativeToken
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
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

    fun loadTokens() = account.network.let { network ->
        listOf(
            NativeToken(
                network.chainId,
                network.name,
                network.token,
                logoRes = getMainTokenIconRes(network.chainId)
            )
        ) + tokenManager.getActiveTokensPerAccount(account)
    }

    fun getTokenVisibilitySettings(tokenAddress: String): Boolean =
        tokenVisibilitySettings.getTokenVisibility(account.address, tokenAddress) ?: false


    fun saveTokenVisibilitySettings(tokenAddress: String, visibility: Boolean) {
        tokenVisibilitySettings = localStorage.saveTokenVisibilitySettings(
            tokenVisibilitySettings.updateTokenVisibility(account.address, tokenAddress, visibility)
        )
    }
}