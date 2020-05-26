package minerva.android.onboarding

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import minerva.android.R
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.launchActivity
import minerva.android.kotlinUtils.Empty
import minerva.android.main.MainActivity
import minerva.android.onboarding.create.CreateWalletFragment
import minerva.android.onboarding.restore.RestoreWalletFragment
import minerva.android.onboarding.welcome.WelcomeFragment

class OnBoardingActivity : AppCompatActivity(), OnBoardingFragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        setupActionBar()
        showWelcomeFragment()
    }

    override fun onBackPressed() {
        if (isRestoreOrCreateWalletFragment()) {
            setToolbarBackButtonVisibility(false)
        }
        super.onBackPressed()
    }

    private fun showWelcomeFragment() {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.mainContent, WelcomeFragment())
            commit()
        }
    }

    private fun isRestoreOrCreateWalletFragment() = getCurrentFragment() is RestoreWalletFragment ||
            getCurrentFragment() is CreateWalletFragment

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = String.Empty
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
        }
        window.statusBarColor = Color.WHITE
    }

    private fun setToolbarBackButtonVisibility(isVisible: Boolean) {
        supportActionBar?.apply {
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

    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.mainContent, fragment)
            addToBackStack(tag)
            commit()
        }
    }

    override fun showRestoreWalletFragment() {
        showFragment(RestoreWalletFragment.newInstance(), RestoreWalletFragment.TAG)
        setToolbarBackButtonVisibility(true)
    }

    override fun showCreateWalletFragment() {
        showFragment(CreateWalletFragment.newInstance(), CreateWalletFragment.TAG)
        setToolbarBackButtonVisibility(true)
    }

    override fun showMainActivity() {
        launchActivity<MainActivity> { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
    }
}
