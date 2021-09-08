package minerva.android.onboarding

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.extension.addFragment
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.launchActivity
import minerva.android.extension.replaceFragmentWithBackStack
import minerva.android.kotlinUtils.Empty
import minerva.android.main.MainActivity
import minerva.android.onboarding.restore.RestoreWalletFragment
import minerva.android.onboarding.welcome.WelcomeFragment

class OnBoardingActivity : AppCompatActivity(), OnBoardingFragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        addFragment(R.id.main_content, WelcomeFragment.newInstance())
    }

    override fun onBackPressed() {
        if (isRestoreOrWelcomeFragment()) {
            setToolbarBackButtonVisibility(false)
        }
        super.onBackPressed()
    }

    private fun isRestoreOrWelcomeFragment() =
        getCurrentFragment() is RestoreWalletFragment || getCurrentFragment() is WelcomeFragment

    private fun setupActionBar(text: String = String.Empty, @ColorRes colorRes: Int = R.color.white) {
        supportActionBar?.apply {
            title = text
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@OnBoardingActivity, colorRes)))
            hide()
        }
        window.statusBarColor = ContextCompat.getColor(this, colorRes)
    }

    private fun setToolbarBackButtonVisibility(isVisible: Boolean) {
        supportActionBar?.apply {
            if (isVisible) show() else hide()
            setDisplayHomeAsUpEnabled(isVisible)
            setDisplayShowHomeEnabled(isVisible)
        }
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (isBackButtonPressed(menuItem)) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun isBackButtonPressed(menuItem: MenuItem) = menuItem.itemId == android.R.id.home

    override fun showRestoreWalletFragment() {
        setupActionBar(getString(R.string.restore_wallet_title), R.color.safeAccountBackground)
        replaceFragmentWithBackStack(R.id.main_content, RestoreWalletFragment.newInstance())
        setToolbarBackButtonVisibility(true)
    }

    override fun showMainActivity() {
        launchActivity<MainActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            finish()
        }
    }

    override fun updateActionBar() {
        setupActionBar()
    }
}
