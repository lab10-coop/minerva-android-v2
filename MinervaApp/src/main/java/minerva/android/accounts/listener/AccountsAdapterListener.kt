package minerva.android.accounts.listener

import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.state.AccountWidgetState

interface AccountsAdapterListener {
    fun onSendCoinClicked(account: Account)
    fun onSendTokenClicked(account: Account, tokenAddress: String, isTokenError: Boolean)
    fun onNftCollectionClicked(account: Account, tokenAddress: String, collectionName: String, isGroup: Boolean = false)
    fun onAccountHide(index: Int)
    fun onCreateSafeAccountClicked(account: Account)
    fun onShowAddress(account: Account)
    fun onShowSafeAccountSettings(account: Account, index: Int)
    fun onWalletConnect(index: Int)
    fun onManageTokens(index: Int)
    fun onExportPrivateKey(account: Account)
    fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState)
    fun getAccountWidgetState(index: Int): AccountWidgetState
    fun onEditName(account: Account)
    /**
     * Open In Explorer - open in browser wallet transactions info by specified account
     * @param account - Account which info we need to show
     */
    fun openInExplorer(account: Account)
}