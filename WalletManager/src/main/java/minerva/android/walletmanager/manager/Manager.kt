package minerva.android.walletmanager.manager

import androidx.lifecycle.LiveData
import minerva.android.walletmanager.model.wallet.WalletConfig

interface Manager {
    val walletConfigLiveData: LiveData<WalletConfig>
}