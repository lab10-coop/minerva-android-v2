package minerva.android.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.history.HistoryFragment
import minerva.android.identities.IdentitiesFragment
import minerva.android.onboarding.OnBoardingActivity
import minerva.android.services.ServicesFragment
import minerva.android.settings.SettingsFragment
import minerva.android.values.ValuesFragment
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //TODO can we do it better? Maybe on SplashScreen?
        checkWalletManagerConfig()

        setContentView(R.layout.activity_main)
        prepareBottomNavMenu()

        replaceFragment(IdentitiesFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        //TODO change menu visibility according to current fragment
        menu?.findItem(R.id.barcodeReader)?.apply {
            isVisible = isIdentitiesTabSelected()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.barcodeReader -> Timber.d("Barcode reader") //TODO add action
            R.id.addIdentities -> Timber.d("Add identities") //TODO add action
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (isIdentitiesTabSelected()) super.onBackPressed()
        else bottomNavigation.selectedItemId = R.id.identities
    }

    private fun isIdentitiesTabSelected() = bottomNavigation.selectedItemId == R.id.identities

    private fun checkWalletManagerConfig() {
        if (!viewModel.initWalletConfig()) {
            launchActivity<OnBoardingActivity> {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
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
}
