package minerva.android.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.extension.launchActivityForResult
import minerva.android.history.HistoryFragment
import minerva.android.identities.IdentitiesFragment
import minerva.android.main.listener.BottomNavigationMenuListener
import minerva.android.onboarding.OnBoardingActivity
import minerva.android.services.ServicesFragment
import minerva.android.services.login.PainlessLoginActivity
import minerva.android.services.login.PainlessLoginActivity.Companion.IS_LOGIN_SUCCESS
import minerva.android.settings.SettingsFragment
import minerva.android.values.ValuesFragment
import minerva.android.widget.MinervaFlashbar
import minerva.wrapped.startNewIdentityWrappedActivity
import org.koin.android.ext.android.inject


class MainActivity : AppCompatActivity(), BottomNavigationMenuListener {

    private val viewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //TODO can we do it better? Maybe on SplashScreen?
        checkMasterSeedAvailability()
        setContentView(R.layout.activity_main)
        prepareBottomNavMenu()
        replaceFragment(IdentitiesFragment())
        prepareSettingsIcon()
    }

    private fun prepareSettingsIcon() {
        if (!viewModel.isMnemonicRemembered()) {
            bottomNavigation.getOrCreateBadge(R.id.settings)
        } else {
            removeSettingsBadgeIcon()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.barcodeScanner)?.apply {
            isVisible = isServicesTabSelected()
        }
        menu?.findItem(R.id.addIdentity)?.apply {
            isVisible = shouldShowAddIdentityIcon()
        }
        menu?.findItem(R.id.addValue)?.apply {
            isVisible = shouldShowAddValueIcon()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addIdentity -> startNewIdentityWrappedActivity(this)
            //TODO implement adding new values
            R.id.addValue -> Toast.makeText(this, "Add Value", Toast.LENGTH_SHORT).show()
            R.id.barcodeScanner -> launchActivityForResult<PainlessLoginActivity>(LOGIN_RESULT_REQUEST_CODE)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getBooleanExtra(IS_LOGIN_SUCCESS, false)?.let { isLoginSuccess ->
                when {
                    isLoginSuccess -> MinervaFlashbar.show(
                        this,
                        getString(R.string.login_success_title),
                        getString(R.string.login_success_message)
                    )
                    !isLoginSuccess -> MinervaFlashbar.show(
                        this,
                        getString(R.string.login_failure_title),
                        getString(R.string.login_failure_message)
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        if (isIdentitiesTabSelected()) super.onBackPressed()
        else bottomNavigation.selectedItemId = R.id.identities
    }

    private fun shouldShowAddIdentityIcon() = isIdentitiesTabSelected()

    private fun shouldShowAddValueIcon() = isValuesTabSelected()

    private fun isServicesTabSelected() = bottomNavigation.selectedItemId == R.id.services

    private fun isValuesTabSelected() = bottomNavigation.selectedItemId == R.id.values

    private fun isIdentitiesTabSelected() = bottomNavigation.selectedItemId == R.id.identities

    private fun checkMasterSeedAvailability() {
        if (!viewModel.isMaskerKeyAvailable()) showOnBoardingActivity()
        else viewModel.initWalletConfig()
    }

    private fun showOnBoardingActivity() {
        launchActivity<OnBoardingActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    private fun replaceFragment(fragment: Fragment, @StringRes title: Int = R.string.identities) {
        supportActionBar?.setTitle(title)
        invalidateOptionsMenu()

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, fragment)
            commit()
        }
    }

    private fun prepareBottomNavMenu() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.identities -> replaceFragment(IdentitiesFragment())
                R.id.values -> replaceFragment(
                    ValuesFragment(),
                    R.string.values
                )
                R.id.services -> replaceFragment(
                    ServicesFragment(),
                    R.string.services
                )
                R.id.activity -> replaceFragment(
                    HistoryFragment(),
                    R.string.activity
                )
                R.id.settings -> replaceFragment(
                    SettingsFragment(),
                    R.string.settings
                )
            }
            true
        }
    }

    override fun removeSettingsBadgeIcon() =
        bottomNavigation.removeBadge(R.id.settings)

    companion object {
        const val LOGIN_RESULT_REQUEST_CODE = 3
    }
}
