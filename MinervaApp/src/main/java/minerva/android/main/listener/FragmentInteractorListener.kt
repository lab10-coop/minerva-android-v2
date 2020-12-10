package minerva.android.main.listener

import minerva.android.walletmanager.model.Account

interface FragmentInteractorListener {
    fun showSendTransactionScreen(index:Int)
    fun showSendAssetTransactionScreen(accountIndex: Int, assetIndex: Int)
    fun shouldShowLoadingScreen(isLoading: Boolean)
    fun changeActionBarColor(color: Int)
    fun removeSettingsBadgeIcon()
}