package minerva.android.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.wallet.WalletManager
import minerva.android.walletmanager.model.WalletConfig

class ServicesViewModel(walletManager: WalletManager) : ViewModel() {
    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData
}