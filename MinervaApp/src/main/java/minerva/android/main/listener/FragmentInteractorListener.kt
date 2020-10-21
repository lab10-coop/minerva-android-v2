package minerva.android.main.listener

import minerva.android.walletmanager.model.Account

interface FragmentInteractorListener {
    fun showSendTransactionScreen(account: Account)
    fun showSendAssetTransactionScreen(accountIndex: Int, assetIndex: Int)
    fun shouldShowLoadingScreen(isLoading: Boolean)
    fun changeActionBarColor(color: Int)
    fun removeSettingsBadgeIcon()
}