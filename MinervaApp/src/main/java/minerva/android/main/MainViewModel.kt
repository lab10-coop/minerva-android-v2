package minerva.android.main

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.WalletManager

class MainViewModel(private val walletManager: WalletManager) : ViewModel() {

    fun isMaskerKeyAvailable() = walletManager.isMasterKeyAvailable()

    fun initWalletConfig() {
        walletManager.initWalletConfig()
    }

    fun isMnemonicRemembered(): Boolean = walletManager.isMnemonicRemembered()
}