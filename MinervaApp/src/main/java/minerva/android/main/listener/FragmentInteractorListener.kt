package minerva.android.main.listener

import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.SEND_TRANSACTION_INDEX
import minerva.android.kotlinUtils.InvalidIndex

interface FragmentInteractorListener {
    fun showTransactionScreen(index: Int, asset: Int = Int.InvalidIndex, screenIndex: Int = SEND_TRANSACTION_INDEX)
    fun shouldShowLoadingScreen(isLoading: Boolean)
    fun changeActionBarColor(color: Int)
    fun removeSettingsBadgeIcon()
    fun showWalletConnectScanner()
}