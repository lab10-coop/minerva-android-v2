package minerva.android.accounts.listener

import minerva.android.walletmanager.model.Account

interface AccountsFragmentToAdapterListener {
    fun onSendTransaction(index: Int)
    fun onSendAssetTransaction(accountIndex: Int, assetIndex: Int)
    fun onCreateSafeAccount(account: Account)
    fun onAccountRemove(account: Account)
    fun onShowAddress(accountIndex: Int)
    fun onShowSafeAccountSettings(account: Account, position: Int)
    fun onWalletConnect(index: Int)
    fun onManageAssets(index: Int)
    fun isAssetVisible(networkAddress: String, assetAddress: String): Boolean?
    fun saveAssetVisibility(networkAddress: String, assetAddress: String, visibility: Boolean)
}