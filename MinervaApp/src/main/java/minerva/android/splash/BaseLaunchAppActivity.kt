package minerva.android.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import minerva.android.extension.launchActivity
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.onboarding.OnBoardingActivity
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.exception.UnableToDecryptMasterSeedThrowable
import minerva.android.walletmanager.exception.UnableToInitializeWalletConfigThrowable
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

abstract class BaseLaunchAppActivity : AppCompatActivity() {

    private val viewModel: LaunchApplicationViewModel by viewModel()

    abstract fun showMainActivity()

    private fun showOnBoardingActivity() {
        launchActivity<OnBoardingActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun handleError(e: Throwable){
        when(e){
            is UnableToInitializeWalletConfigThrowable -> {
                viewModel.restoreWalletConfigWithSavedMasterSeed()
            }
            is UnableToDecryptMasterSeedThrowable -> {
                showOnBoardingActivity()
            }
        }
    }

    protected fun checkWalletConfig() {
        try {
            viewModel.getWalletConfig()
            showMainActivity()
        } catch (error: NotInitializedWalletConfigThrowable) {
            Timber.e(error)
            initiateWalletConfig()
            viewModel.walletConfigLiveData.observe(this@BaseLaunchAppActivity, EventObserver { showMainActivity() })
        }
        viewModel.walletConfigErrorLiveData.observe(this@BaseLaunchAppActivity, EventObserver { handleError(it) })
    }

    protected fun checkWalletConfigWithOnlyObservers() {
        try {
            viewModel.getWalletConfig()
            showMainActivity()
        } catch (error: NotInitializedWalletConfigThrowable) {
            viewModel.walletConfigLiveData.observe(this@BaseLaunchAppActivity, EventObserver {
                showMainActivity()
            })
        }
        viewModel.walletConfigErrorLiveData.observe(this@BaseLaunchAppActivity, EventObserver { handleError(it) })
    }

    protected fun initiateWalletConfig() {
        viewModel.initWalletConfig()
    }
}