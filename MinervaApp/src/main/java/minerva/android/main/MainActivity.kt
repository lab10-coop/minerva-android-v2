package minerva.android.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import minerva.android.R
import minerva.android.extension.launchActivity
import minerva.android.extension.launchActivityForResult
import minerva.android.identities.IdentitiesFragment
import minerva.android.main.handler.*
import minerva.android.main.listener.BottomNavigationMenuListener
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.onboarding.OnBoardingActivity
import minerva.android.services.login.PainlessLoginActivity
import minerva.android.values.transaction.activity.TransactionActivity
import minerva.android.values.transaction.activity.TransactionActivity.Companion.VALUE_INDEX
import minerva.android.walletmanager.model.Value
import minerva.android.wrapped.startNewIdentityWrappedActivity
import minerva.android.wrapped.startNewValueWrappedActivity
import org.koin.android.ext.android.inject


class MainActivity : AppCompatActivity(), BottomNavigationMenuListener, FragmentInteractorListener {

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

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
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
            R.id.addValue -> startNewValueActivity()
            R.id.barcodeScanner -> launchActivityForResult<PainlessLoginActivity>(LOGIN_RESULT_REQUEST_CODE)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (isLoginResult(requestCode, resultCode)) {
            handleLoginResult(data)
        } else if (isTransactionResult(requestCode, resultCode)) {
            handleTransactionResult(data)
        }
    }

    override fun showSendTransactionScreen(value: Value) {
        launchActivityForResult<TransactionActivity>(TRANSACTION_RESULT_REQUEST_CODE) {
            putExtra(VALUE_INDEX, value.index)
        }
    }

    override fun showSendAssetTransactionScreen() {
        //TODO implement sending assets
        Toast.makeText(this, "Showing send asset transaction screen", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (isIdentitiesTabSelected()) super.onBackPressed()
        else bottomNavigation.selectedItemId = R.id.identities
    }

    override fun removeSettingsBadgeIcon() =
        bottomNavigation.removeBadge(R.id.settings)

    private fun checkMasterSeedAvailability() {
        if (!viewModel.isMasterKeyAvailable()) showOnBoardingActivity()
        else viewModel.initWalletConfig()
    }

    private fun showOnBoardingActivity() {
        launchActivity<OnBoardingActivity> {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    private fun startNewValueActivity() {
        startNewValueWrappedActivity(
            this,
            String.format(NEW_VALUE_TITLE_PATTERN, getString(R.string.new_account), viewModel.getValueIterator()),
            viewModel.getValueIterator()
        )
    }

    companion object {
        const val LOGIN_RESULT_REQUEST_CODE = 3
        private const val NEW_VALUE_TITLE_PATTERN = "%s #%d"
        const val TRANSACTION_RESULT_REQUEST_CODE = 4
    }
}
