package minerva.android.token

import androidx.annotation.VisibleForTesting
import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.AssetBalance
import minerva.android.walletmanager.model.token.*
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.widget.repository.getMainTokenIconRes
import java.math.BigDecimal

class ManageTokensViewModel(
    private val accountManager: AccountManager,
    private val localStorage: LocalStorage,
    private val tokenManager: TokenManager,
    private val transactionRepository: TransactionRepository
) : BaseViewModel() {

    lateinit var account: Account

    @VisibleForTesting
    lateinit var tokenVisibilitySettings: TokenVisibilitySettings

    fun initViewModel(index: Int) {
        account = accountManager.loadAccount(index)
        tokenVisibilitySettings = localStorage.getTokenVisibilitySettings()
    }

    fun loadTokens() = account.network.let { network ->
        mutableListOf<Token>(
            NativeToken(
                network.chainId,
                network.name,
                network.token,
                logoRes = getMainTokenIconRes(network.chainId)
            )
        ).apply {
            account.accountTokens
                .distinctBy { token -> token.token.address }
                .sortedWith(
                    compareBy(
                        {
                            if (it.token.type.isERC20()) {
                                1
                            } else if (it.token.type.isNft()) {
                                2
                            } else {
                                3
                            }
                        },
                        { it.token.logoURI.isNullOrEmpty() },
                        { it.token.symbol }
                    )
                )
                .forEach { token ->
                    add(token.token)
                }
        }
    }

    fun getTokenVisibilitySettings(tokenAddress: String): Boolean =
        tokenVisibilitySettings.getTokenVisibility(account.address, tokenAddress) ?: false


    fun saveTokenVisibilitySettings(tokenAddress: String, visibility: Boolean) {
        tokenVisibilitySettings = localStorage.saveTokenVisibilitySettings(
            tokenVisibilitySettings.updateTokenVisibility(account.address, tokenAddress, visibility)
        )
    }
}