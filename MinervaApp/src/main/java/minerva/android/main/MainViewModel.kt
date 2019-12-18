package minerva.android.main

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.WalletManager

class MainViewModel(private val walletManager: WalletManager) : ViewModel() {

    fun initWalletConfig() = walletManager.initWalletConfig()

}