package minerva.android.accounts.listener

import minerva.android.walletmanager.model.Account

interface AccountsAdapterListener {
    fun onSendAccountClicked(account: Account)
    fun onSendAssetClicked(accountIndex: Int, assetIndex: Int)
    fun onAccountRemoved(position: Int)
    fun onCreateSafeAccountClicked(account: Account)
    fun onShowAddress(account: Account, position: Int)
    fun onShowSafeAccountSettings(account: Account, position: Int)
    fun onWalletConnect()
}