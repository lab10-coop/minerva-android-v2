package minerva.android.main.listener

import minerva.android.walletmanager.model.Account

interface FragmentInteractorListener {
    fun showSendTransactionScreen(account: Account)
    fun showSendAssetTransactionScreen(valueIndex: Int, assetIndex: Int)
    fun shouldShowLoadingScreen(isLoading: Boolean)
}