package minerva.android.main.listener

import minerva.android.walletmanager.model.Value

interface FragmentInteractorListener {
    fun showSendTransactionScreen(value: Value)
    fun showSendAssetTransactionScreen(valueIndex: Int, assetIndex: Int)
    fun shouldShowLoadingScreen(isLoading: Boolean)
}