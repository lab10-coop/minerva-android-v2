package minerva.android.accounts.listener

import minerva.android.walletmanager.model.minervaprimitives.account.Account

interface AccountsAdapterListener {
    fun onSendAccountClicked(account: Account)
    fun onSendTokenClicked(account: Account, assetIndex: Int)
    fun onAccountRemoved(index: Int)
    fun onCreateSafeAccountClicked(account: Account)
    fun onShowAddress(account: Account)
    fun onShowSafeAccountSettings(account: Account, index: Int)
    fun onWalletConnect(index: Int)
    fun onManageTokens(index: Int)
    fun onExportPrivateKey(account: Account)
    fun updateAccountWidgetState(index: Int, isOpen: Boolean)
    fun getAccountWidgetState(index: Int): Boolean
}