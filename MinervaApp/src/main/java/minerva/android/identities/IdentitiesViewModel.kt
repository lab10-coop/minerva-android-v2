package minerva.android.identities

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.WalletConfig

class IdentitiesViewModel(walletManager: WalletManager) : ViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData
}