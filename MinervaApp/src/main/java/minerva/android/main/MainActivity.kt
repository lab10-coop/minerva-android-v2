package minerva.android.main

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import minerva.android.R
import minerva.android.accounts.AccountsFragment
import minerva.android.accounts.transaction.activity.TransactionActivity
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.ACCOUNT_INDEX
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.ASSET_INDEX
import minerva.android.extension.getCurrentFragment
import minerva.android.extension.launchActivityForResult
import minerva.android.extension.visibleOrGone
import minerva.android.identities.IdentitiesFragment
import minerva.android.identities.credentials.CredentialsFragment
import minerva.android.identities.myIdentities.MyIdentitiesFragment
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.handler.*
import minerva.android.main.listener.BottomNavigationMenuListener
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.services.login.LoginScannerActivity
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.wrapped.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(), BottomNavigationMenuListener, FragmentInteractorListener {

    internal val viewModel: MainViewModel by inject()
    private var shouldDisableAddButton = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prepareBottomNavMenu()
        replaceFragment(IdentitiesFragment())
        prepareSettingsIcon()
        prepareObservers()
        painlessLogin(intent)
    }

    override fun onResume() {
        super.onResume()
        shouldShowLoadingScreen(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        painlessLogin(intent)
    }

    private fun prepareObservers() {
        viewModel.apply {
            notExistedIdentityLiveData.observe(this@MainActivity, EventObserver {
                Toast.makeText(this@MainActivity, getString(R.string.not_existed_identity_message), Toast.LENGTH_LONG).show()
            })
            requestedFieldsLiveData.observe(this@MainActivity, EventObserver {
                Toast.makeText(this@MainActivity, getString(R.string.fill_requested_data_message, it), Toast.LENGTH_LONG).show()
            })
            errorLiveData.observe(this@MainActivity, EventObserver {
                Toast.makeText(this@MainActivity, getString(R.string.unexpected_error), Toast.LENGTH_LONG).show()
            })
            loadingLiveData.observe(this@MainActivity, EventObserver {
                (getCurrentFragment() as AccountsFragment).setProgressAccount(it.first, it.second)
            })
            updateCredentialSuccessLiveData.observe(this@MainActivity, EventObserver {
                showBindCredentialFlashbar(true, it)
            })
            updateCredentialErrorLiveData.observe(this@MainActivity, EventObserver {
                showBindCredentialFlashbar(false, null)
            })
        }
    }

    private fun painlessLogin(intent: Intent?) {
        viewModel.loginFromNotification(intent?.getStringExtra(JWT))
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
        with(viewModel) {
            menu?.apply {
                findItem(R.id.editIdentityOrder)?.isVisible =
                    shouldShowAddIdentityIcon() && isOrderEditAvailable(WalletActionType.IDENTITY) && getCurrentChildFragment() is MyIdentitiesFragment
                findItem(R.id.addIdentity)?.isVisible = shouldShowAddIdentityIcon() && getCurrentChildFragment() is MyIdentitiesFragment
                findItem(R.id.editAccountOrder)?.isVisible = shouldShowAddValueIcon() && isOrderEditAvailable(WalletActionType.ACCOUNT)
                findItem(R.id.addAccount)?.apply {
                    isVisible = shouldShowAddValueIcon()
                    isEnabled = !shouldDisableAddButton
                }
                findItem(R.id.editServiceOrder)?.isVisible = isServicesTabSelected() && isOrderEditAvailable(WalletActionType.SERVICE)
                findItem(R.id.editCredentialsOrder)?.isVisible = isIdentitiesTabSelected() &&
                        getCurrentChildFragment() is CredentialsFragment && isOrderEditAvailable(WalletActionType.CREDENTIAL)
                findItem(R.id.qrCodeScanner)?.isVisible = isServicesTabSelected() ||
                        (isIdentitiesTabSelected() && getCurrentChildFragment() is CredentialsFragment)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun getCurrentChildFragment() =
        (getCurrentFragment() as? IdentitiesFragment)?.currentFragment.orElse { MyIdentitiesFragment.newInstance() }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.editIdentityOrder -> startEditIdentityOrderWrappedActivity(this)
            R.id.addIdentity -> startNewIdentityWrappedActivity(this)
            R.id.editAccountOrder -> startEditAccountOrderWrappedActivity(this)
            R.id.editCredentialsOrder -> startEditCredentialOrderWrappedActivity(this)
            R.id.addAccount -> startNewAccountActivity()
            R.id.editServiceOrder -> startEditServiceOrderWrappedActivity(this)
            R.id.qrCodeScanner -> launchActivityForResult<LoginScannerActivity>(LOGIN_SCANNER_RESULT_REQUEST_CODE)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            isLoginScannerResult(requestCode, resultCode) -> handleLoginScannerResult(data)
            isTransactionPrepared(requestCode, resultCode) -> handlePreparedTransaction(data)
        }
    }

    private fun handlePreparedTransaction(data: Intent?) {
        data?.apply {
            handleTransactionResult(data)
            return
            //TODO Pending UI - skipped for now (logic needs to be implemented)
            getIntExtra(ACCOUNT_INDEX, Int.InvalidValue).let {
                if (it != Int.InvalidValue) {
                    (getCurrentFragment() as AccountsFragment).setProgressAccount(it, true)
                    //TODO add transaction stage 2!
                }
            }
        }
    }

    override fun showSendTransactionScreen(account: Account) {
        launchActivityForResult<TransactionActivity>(TRANSACTION_RESULT_REQUEST_CODE) {
            putExtra(ACCOUNT_INDEX, account.index)
        }
    }

    override fun showSendAssetTransactionScreen(valueIndex: Int, assetIndex: Int) {
        launchActivityForResult<TransactionActivity>(TRANSACTION_RESULT_REQUEST_CODE) {
            putExtra(ACCOUNT_INDEX, valueIndex)
            putExtra(ASSET_INDEX, assetIndex)
        }
    }

    override fun shouldShowLoadingScreen(isLoading: Boolean) {
        loadingScreen.apply {
            visibleOrGone(isLoading)
            startAnimation()
        }
        toggleLoadingActionBar(isLoading)
        toggleAddValueButton(isLoading)
    }

    private fun toggleLoadingActionBar(isLoading: Boolean) {
        if (isLoading) setLoadingActionBar(R.color.loadingScreenBackground)
        else setLoadingActionBar(R.color.lightGray)
    }

    private fun toggleAddValueButton(isLoading: Boolean) {
        shouldDisableAddButton = isLoading
        invalidateOptionsMenu()
    }

    private fun setLoadingActionBar(color: Int) {
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(color)))
        window.statusBarColor = getColor(color)
    }

    override fun onBackPressed() {
        if (isIdentitiesTabSelected()) super.onBackPressed()
        else bottomNavigation.selectedItemId = R.id.identities
    }

    override fun removeSettingsBadgeIcon() =
        bottomNavigation.removeBadge(R.id.settings)

    private fun startNewAccountActivity() {
        startNewAccountWrappedActivity(
            this,
            String.format(NEW_VALUE_TITLE_PATTERN, getString(R.string.new_account), viewModel.getValueIterator()),
            viewModel.getValueIterator()
        )
    }

    fun onPainlessLogin() {
        viewModel.painlessLogin()
    }

    fun updateCredential() {
        viewModel.updateBindedCredential()
    }

    fun onAllowNotifications(shouldLogin: Boolean) {
        if (shouldLogin) viewModel.painlessLogin()
        else Toast.makeText(this, "Allow push notifications, will be added soon", Toast.LENGTH_SHORT).show()
        //        TODO send to API that push notifications are allowed
    }

    fun onDenyNotifications() {
//        TODO send to API that push notifications are no longer allowed
        Toast.makeText(this, "Disable push notifications, will be added soon", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val NEW_VALUE_TITLE_PATTERN = "%s #%d"
        const val LOGIN_SCANNER_RESULT_REQUEST_CODE = 3
        const val TRANSACTION_RESULT_REQUEST_CODE = 4
        const val JWT = "jwt"
    }
}