package minerva.android.walletmanager.manager

import androidx.lifecycle.LiveData
import minerva.android.walletmanager.model.WalletConfig

interface Manager {
    val walletConfigLiveData: LiveData<WalletConfig>
}