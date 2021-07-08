package minerva.android.splash

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash_screen.*
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.MainActivity
import minerva.android.onboarding.OnBoardingActivity
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SplashScreenActivity : AppCompatActivity(), PassiveVideoToActivityInteractor {

    private val viewModel: SplashScreenViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        setupActionBar()
        with(passiveVideoView) {
            onCreate(this@SplashScreenActivity)
            setupListener(this@SplashScreenActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        passiveVideoView.start()
    }

    override fun onPause() {
        super.onPause()
        passiveVideoView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        passiveVideoView.onDestroy()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = String.Empty
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
        }
        window.statusBarColor = Color.WHITE
    }

    private fun showMainActivity() {
        launchActivity<MainActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    private fun showOnBoardingActivity() {
        launchActivity<OnBoardingActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onAnimationEnd() {
        try {
            viewModel.getWalletConfig()
            showMainActivity()
        } catch (error: NotInitializedWalletConfigThrowable) {
            Timber.e(error)
            initWalletConfig()
            viewModel.walletConfigLiveData.observe(this@SplashScreenActivity, EventObserver { showMainActivity() })
        }
        viewModel.walletConfigErrorLiveData.observe(this@SplashScreenActivity, EventObserver { showOnBoardingActivity() })
    }

    override fun initWalletConfig() {
        viewModel.initWalletConfig()
    }
}