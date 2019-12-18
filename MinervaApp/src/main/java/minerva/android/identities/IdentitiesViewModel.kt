package minerva.android.identities

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletConfig

class IdentitiesViewModel(private val walletManager: WalletManager) : ViewModel() {

    fun walletConfigLiveData(): LiveData<WalletConfig> = walletManager.walletConfigLiveData
}