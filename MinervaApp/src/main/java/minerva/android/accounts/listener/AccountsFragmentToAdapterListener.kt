package minerva.android.accounts.listener

import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.state.AppUIStateImpl

interface AccountsFragmentToAdapterListener {
    fun onSendTransaction(index: Int)
    fun onSendTokenTransaction(accountIndex: Int, tokenIndex: Int)
    fun onCreateSafeAccount(account: Account)
    fun onAccountRemove(account: Account)
    fun onShowAddress(accountIndex: Int)
    fun onShowSafeAccountSettings(account: Account, position: Int)
    fun onWalletConnect(index: Int)
    fun onManageTokens(index: Int)
    fun onExportPrivateKey(account: Account)
    fun updateAccountWidgetState(index: Int, isOpen: Boolean)
    fun getAccountWidgetState(index: Int): Boolean
}