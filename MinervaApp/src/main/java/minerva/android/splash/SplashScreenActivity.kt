package minerva.android.splash

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_splash_screen.*
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.kotlinUtils.Empty
import minerva.android.main.MainActivity

class SplashScreenActivity : BaseLaunchAppActivity(), PassiveVideoToActivityInteractor {

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

    override fun onAnimationEnd() {
        checkWalletConnect()
    }

    override fun initWalletConfig() {
        initiateWalletConfig()
    }

    override fun showMainActivity() {
        launchActivity<MainActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            finish()
        }
    }
}