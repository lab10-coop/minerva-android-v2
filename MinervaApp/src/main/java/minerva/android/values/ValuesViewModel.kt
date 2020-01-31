package minerva.android.values

import androidx.lifecycle.LiveData
import minerva.android.kotlinUtils.viewmodel.BaseViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.WalletConfig
import timber.log.Timber
import java.math.BigDecimal

class ValuesViewModel(private val walletManager: WalletManager) : BaseViewModel() {

    val walletConfigLiveData: LiveData<WalletConfig> = walletManager.walletConfigLiveData

    val balanceLiveData: LiveData<HashMap<String, BigDecimal>> = walletManager.balanceLiveData

    fun refreshBalances() {
        walletManager.refreshBalances()
    }
}