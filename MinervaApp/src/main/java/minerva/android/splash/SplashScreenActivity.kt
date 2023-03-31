package minerva.android.splash

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import com.airbnb.lottie.LottieAnimationView
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.main.MainActivity

class SplashScreenActivity : BaseLaunchAppActivity(), PassiveVideoToActivityInteractor {

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_splash_screen)
        super.onCreate(savedInstanceState)
        setupStatusBar()
    }

    override fun onResume() {
        super.onResume()
        Handler().postDelayed({
            findViewById<LottieAnimationView>(R.id.animation_view).playAnimation()
        }, ANIMATION_DELAY)
        setupAnimationListeners()
    }

    private fun setupAnimationListeners() {
        findViewById<LottieAnimationView>(R.id.animation_view).addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                initWalletConfig()
            }

            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd()
            }

            override fun onAnimationCancel(animation: Animator) {
                // nothing to do
            }

            override fun onAnimationRepeat(animation: Animator) {
                // nothing to do
            }
        })
    }

    private fun setupStatusBar() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onAnimationEnd() {
        checkWalletConfigWithOnlyObservers()
    }

    override fun initWalletConfig() {
        initiateWalletConfig()
    }

    override fun showMainActivity() {
        launchActivity<MainActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            finish()
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        private const val ANIMATION_DELAY = 500L
    }
}