package minerva.android.app

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.widget.state.AppUIState

class AppViewModel(
    private val walletConfigManager: WalletConfigManager,
    private val appUIState: AppUIState
) : ViewModel() {

    fun checkWalletConfigInitialization() {
        try {
            walletConfigManager.getWalletConfig()
        } catch (error: NotInitializedWalletConfigThrowable) {
            appUIState.shouldShowSplashScreen = true
        }
    }
}