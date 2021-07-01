package minerva.android.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.widget.state.AppUIState

class SplashScreenViewModel(
    private val masterSeedRepository: MasterSeedRepository,
    appUIState: AppUIState
) : ViewModel() {

    init {
        appUIState.shouldShowSplashScreen = false
    }

    val walletConfigErrorLiveData: LiveData<Event<Throwable>> = masterSeedRepository.walletConfigErrorLiveData
    val walletConfigLiveData: LiveData<Event<WalletConfig>> = masterSeedRepository.walletConfigLiveData

    fun initWalletConfig() {
        masterSeedRepository.initWalletConfig()
    }

    fun getWalletConfig() = masterSeedRepository.getWalletConfig()
}