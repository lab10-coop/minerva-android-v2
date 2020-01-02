package minerva.android.settings

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.WalletManager

class SettingsViewModel(private val walletManager: WalletManager) : ViewModel() {

    fun isMnemonicRemembered() = walletManager.isMnemonicRemembered()
}