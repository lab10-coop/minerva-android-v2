package minerva.android.onboarding

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import minerva.android.R
import minerva.android.extension.addFragment
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.launchActivity
import minerva.android.extension.replaceFragment
import minerva.android.kotlinUtils.Empty
import minerva.android.main.MainActivity
import minerva.android.onboarding.create.CreateWalletFragment
import minerva.android.onboarding.restore.RestoreWalletFragment
import minerva.android.onboarding.welcome.WelcomeFragment
import minerva.android.values.transaction.fragment.scanner.AddressScannerFragment

class OnBoardingActivity : AppCompatActivity(), OnBoardingFragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        setupActionBar()
        addFragment(R.id.mainContent, WelcomeFragment.newInstance())
    }

    override fun onBackPressed() {
        if (isRestoreOrCreateWalletFragment()) {
            setToolbarBackButtonVisibility(false)
        }
        super.onBackPressed()
    }

    private fun isRestoreOrCreateWalletFragment() =
        getCurrentFragment() is RestoreWalletFragment || getCurrentFragment() is CreateWalletFragment

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

    override fun showRestoreWalletFragment() {
        replaceFragment(R.id.mainContent, RestoreWalletFragment.newInstance())
        setToolbarBackButtonVisibility(true)
    }

    override fun showCreateWalletFragment() {
        replaceFragment(R.id.mainContent, CreateWalletFragment.newInstance())
        setToolbarBackButtonVisibility(true)
    }

    override fun showMainActivity() {
        launchActivity<MainActivity> { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
    }
}
