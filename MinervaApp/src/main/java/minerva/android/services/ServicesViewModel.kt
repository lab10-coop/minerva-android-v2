package minerva.android.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.services.ServiceManager
import minerva.android.walletmanager.model.WalletConfig

class ServicesViewModel(serviceManager: ServiceManager) : ViewModel() {
    val walletConfigLiveData: LiveData<WalletConfig> = serviceManager.walletConfigLiveData
}