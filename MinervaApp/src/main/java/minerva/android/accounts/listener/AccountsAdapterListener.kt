package minerva.android.accounts.listener

import minerva.android.walletmanager.model.Account

interface AccountsAdapterListener {
    fun onSendAccountClicked(account: Account)
    fun onSendAssetTokenClicked(account: Account, assetIndex: Int)
    fun onAccountRemoved(index: Int)
    fun onCreateSafeAccountClicked(account: Account)
    fun onShowAddress(account: Account)
    fun onShowSafeAccountSettings(account: Account, index: Int)
    fun onWalletConnect(index: Int)
    fun onManageAssets(index: Int)
    fun onOpenOrClose(index: Int, isOpen: Boolean)
}