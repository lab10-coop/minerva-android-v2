package minerva.android.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.repository.seed.MasterSeedRepository

class SplashScreenViewModel(private val masterSeedRepository: MasterSeedRepository) : ViewModel() {

    val walletConfigErrorLiveData: LiveData<Event<Throwable>> = masterSeedRepository.walletConfigErrorLiveData
    val walletConfigLiveData: LiveData<WalletConfig> = masterSeedRepository.walletConfigLiveData

    fun initWalletConfig() {
        masterSeedRepository.initWalletConfig()
    }
}