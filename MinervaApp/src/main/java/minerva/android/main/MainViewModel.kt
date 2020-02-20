package minerva.android.main

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager

class MainViewModel(private val walletManager: WalletManager) : ViewModel() {

    fun isMasterKeyAvailable() = walletManager.isMasterKeyAvailable()

    fun initWalletConfig() = walletManager.initWalletConfig()

    fun isMnemonicRemembered(): Boolean = walletManager.isMnemonicRemembered()

    fun getValueIterator(): Int = walletManager.getValueIterator()

    fun dispose() = walletManager.dispose()
}