package minerva.android.main.listener

import minerva.android.kotlinUtils.InvalidIndex

interface FragmentInteractorListener {
    fun showTransactionScreen(index: Int, asset: Int = Int.InvalidIndex)
    fun shouldShowLoadingScreen(isLoading: Boolean)
    fun changeActionBarColor(color: Int)
    fun removeSettingsBadgeIcon()
    fun showWalletConnectScanner()
}