package minerva.android.accounts.listener

import minerva.android.walletmanager.model.Account

interface AccountsAdapterListener {
    fun onSendAccountClicked(account: Account)
    fun onSendAssetTokenClicked(accountIndex: Int, assetIndex: Int)
    fun onAccountRemoved(index: Int)
    fun onCreateSafeAccountClicked(account: Account)
    fun onShowAddress(account: Account, index: Int)
    fun onShowSafeAccountSettings(account: Account, index: Int)
    fun onWalletConnect()
}