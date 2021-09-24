package minerva.android.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import minerva.android.extension.launchActivity
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.onboarding.OnBoardingActivity
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

abstract class BaseLaunchAppActivity : AppCompatActivity() {

    private val viewModel: LaunchApplicationViewModel by viewModel()

    abstract fun showMainActivity()

    private fun showOnBoardingActivity() {
        launchActivity<OnBoardingActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    protected fun checkWalletConnect() {
        try {
            viewModel.getWalletConfig()
            showMainActivity()
        } catch (error: NotInitializedWalletConfigThrowable) {
            Timber.e(error)
            initiateWalletConfig()
            viewModel.walletConfigLiveData.observe(this@BaseLaunchAppActivity, EventObserver { showMainActivity() })
        }
        viewModel.walletConfigErrorLiveData.observe(this@BaseLaunchAppActivity, EventObserver { showOnBoardingActivity() })
    }

    protected fun initiateWalletConfig() {
        viewModel.initWalletConfig()
    }
}