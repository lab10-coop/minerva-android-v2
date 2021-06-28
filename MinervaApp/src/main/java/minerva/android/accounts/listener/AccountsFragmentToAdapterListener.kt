package minerva.android.accounts.listener

import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.widget.state.AccountWidgetState

interface AccountsFragmentToAdapterListener {
    fun onSendTransaction(index: Int)
    fun onSendTokenTransaction(accountIndex: Int, tokenAddress: String)
    fun onCreateSafeAccount(account: Account)
    fun onAccountHide(account: Account)
    fun onShowAddress(accountIndex: Int)
    fun onShowSafeAccountSettings(account: Account, position: Int)
    fun onWalletConnect(index: Int)
    fun onManageTokens(index: Int)
    fun onExportPrivateKey(account: Account)
    fun updateAccountWidgetState(index: Int, isOpen: AccountWidgetState)
    fun getAccountWidgetState(index: Int): AccountWidgetState
    fun getTokens(account: Account): List<ERC20Token>
}