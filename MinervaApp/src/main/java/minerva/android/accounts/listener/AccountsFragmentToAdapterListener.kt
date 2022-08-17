package minerva.android.accounts.listener

import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.widget.state.AccountWidgetState

interface AccountsFragmentToAdapterListener {
    fun onSendTransaction(account: Account)
    fun onSendTokenTransaction(account: Account, tokenAddress: String, isTokenError: Boolean)
    fun onNftCollectionClicked(account: Account, tokenAddress: String, collectionName: String, isGroup: Boolean = false)
    fun onCreateSafeAccount(account: Account)
    fun onAccountHide(index: Int)
    fun onShowAddress(account: Account)
    fun onShowSafeAccountSettings(account: Account, position: Int)
    fun onWalletConnect(index: Int)
    fun onManageTokens(index: Int)
    fun onExportPrivateKey(account: Account)
    fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState)
    fun getAccountWidgetState(index: Int): AccountWidgetState
    fun getTokens(account: Account): List<AccountToken>
    fun onEditName(account: Account)
    fun updateSessionCount(
        sessionsPerAccount: List<DappSessionData>,
        passIndex: (index: Int) -> Unit
    )

    fun showPendingAccount(
        index: Int,
        chainId: Int,
        areMainNetsEnabled: Boolean,
        isPending: Boolean,
        passIndex: (index: Int) -> Unit
    )

    fun indexOf(account: Account): Int
    fun stopPendingAccounts()

    /**
     * Open In Explorer - open in browser wallet transactions info by specified account
     * @param account - Account which info we need to show
     */
    fun openInExplorer(account: Account)
}