package minerva.android.accounts.listener

import minerva.android.walletmanager.model.minervaprimitives.account.Account

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
    fun isTokenVisible(networkAddress: String, tokenAddress: String): Boolean?
    fun saveTokenVisibility(networkAddress: String, tokenAddress: String, visibility: Boolean)
}