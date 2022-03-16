package minerva.android.splash

import androidx.lifecycle.LiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.base.BaseViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.widget.state.AppUIState
import timber.log.Timber

class LaunchApplicationViewModel(
    private val masterSeedRepository: MasterSeedRepository,
    private val logger: Logger,
    appUIState: AppUIState
) : BaseViewModel() {

    init {
        appUIState.shouldShowSplashScreen = false
    }

    val walletConfigErrorLiveData: LiveData<Event<Throwable>> = masterSeedRepository.walletConfigErrorLiveData
    val walletConfigLiveData: LiveData<Event<WalletConfig>> = masterSeedRepository.walletConfigLiveData

    fun initWalletConfig() {
        masterSeedRepository.initWalletConfig()
    }

    fun getWalletConfig() = masterSeedRepository.getWalletConfig()

    fun restoreWalletConfigWithSavedMasterSeed() {
        launchDisposable {
            masterSeedRepository.restoreWalletConfigWithSavedMasterSeed()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        initWalletConfig()
                        masterSeedRepository.saveIsMnemonicRemembered()
                    },
                    onError = { error ->
                        Timber.e(error)
                        logger.logToFirebase(error.message ?: "RestoreWalletConfigWithSavedMasterSeed failed")
                    }
                )
        }
    }
}