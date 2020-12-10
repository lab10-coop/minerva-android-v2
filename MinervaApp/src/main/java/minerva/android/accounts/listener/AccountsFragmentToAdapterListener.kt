package minerva.android.accounts.listener

import minerva.android.walletmanager.model.Account

interface AccountsFragmentToAdapterListener {
    fun onSendTransaction(index: Int)
    fun onSendAssetTransaction(accountIndex: Int, assetIndex: Int)
    fun onCreateSafeAccount(account: Account)
    fun onAccountRemove(account: Account)
    fun onShowAddress(account: Account, index: Int)
    fun onShowSafeAccountSettings(account: Account, position: Int)
    fun onWalletConnect()
}