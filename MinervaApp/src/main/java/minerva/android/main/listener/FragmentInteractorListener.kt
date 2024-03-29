package minerva.android.main.listener

import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.SEND_TRANSACTION_INDEX
import minerva.android.kotlinUtils.Empty

interface FragmentInteractorListener {
    fun showTransactionScreen(
        index: Int,
        tokenAddress: String = String.Empty,
        screenIndex: Int = SEND_TRANSACTION_INDEX,
        isCoinBalanceError: Boolean = false,
        isTokenBalanceError: Boolean = false
    )

    fun shouldShowLoadingScreen(isLoading: Boolean)
    fun changeActionBarColor(color: Int)
    fun removeSettingsBadgeIcon()
    fun showWalletConnectScanner(index: Int)
    fun showNftCollectionScreen(index: Int, tokenAddress: String, collectionName: String, isGroup: Boolean = false)
}