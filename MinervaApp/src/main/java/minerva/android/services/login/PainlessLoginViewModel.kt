package minerva.android.services.login

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.WalletManager

class PainlessLoginViewModel(private val walletManager: WalletManager) : ViewModel() {
    fun getIdentities() = walletManager.walletConfigLiveData.value?.identities
}