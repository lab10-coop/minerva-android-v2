package minerva.android.onboarding

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.MasterKey

class OnBoardingViewModel(private val walletManager: WalletManager) : ViewModel() {

    fun saveMasterKey(masterKey: MasterKey) = walletManager.saveMasterKey(masterKey)

}