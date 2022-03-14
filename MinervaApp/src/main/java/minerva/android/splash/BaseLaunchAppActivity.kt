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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    protected fun checkWalletConfig() {
        try {
            Timber.tag("MIGRATION").e("checkWalletConfig 0")
            viewModel.getWalletConfig()
            Timber.tag("MIGRATION").e("checkWalletConfig 1")
            showMainActivity()
            Timber.tag("MIGRATION").e("checkWalletConfig 2")
        } catch (error: NotInitializedWalletConfigThrowable) {
            Timber.e(error)
            Timber.tag("MIGRATION").e("checkWalletConfig 3")
            initiateWalletConfig()
            Timber.tag("MIGRATION").e("checkWalletConfig 4")
            viewModel.walletConfigLiveData.observe(this@BaseLaunchAppActivity, EventObserver { showMainActivity() })
        }
        viewModel.walletConfigErrorLiveData.observe(this@BaseLaunchAppActivity, EventObserver { showOnBoardingActivity() })
    }

    protected fun initiateWalletConfig() {
        viewModel.initWalletConfig()
    }
}