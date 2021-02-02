package minerva.android.main

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import minerva.android.R
import minerva.android.accounts.transaction.activity.TransactionActivity
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.ASSET_INDEX
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.TRANSACTION_MESSAGE
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.TRANSACTION_SCREEN
import minerva.android.accounts.transaction.fragment.AccountsFragment
import minerva.android.accounts.walletconnect.WalletConnectActivity
import minerva.android.databinding.ActivityMainBinding
import minerva.android.extension.*
import minerva.android.identities.IdentitiesFragment
import minerva.android.identities.credentials.CredentialsFragment
import minerva.android.identities.myIdentities.MyIdentitiesFragment
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.base.BaseFragment
import minerva.android.main.handler.*
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.services.login.LoginScannerActivity
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager.getNetwork
import minerva.android.walletmanager.model.PendingAccount
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.repository.walletconnect.OnEthSign
import minerva.android.widget.MinervaFlashbar
import minerva.android.wrapped.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainActivity : AppCompatActivity(), FragmentInteractorListener {

    internal val viewModel: MainViewModel by inject()
    private var shouldDisableAddButton = false
    internal lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prepareBottomNavMenu()
        replaceFragment(IdentitiesFragment())
        prepareSettingsIcon()
        prepareSettingsIcon()
        prepareObservers()
        viewModel.restorePendingTransactions()
        if (!viewModel.isBackupAllowed) {
            AlertDialogHandler.showDialog(
                this,
                getString(R.string.error_header),
                getString(R.string.outdated_wallet_error_message)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        shouldShowLoadingScreen(false)
        handleExecutedAccounts()
    }

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is BaseFragment) {
            fragment.setListener(this)
        }
    }

    private fun handleExecutedAccounts() {
        with(viewModel.executedAccounts) {
            if (isNotEmpty()) {
                forEach {
                    showFlashbar(
                        getString(R.string.transaction_success_title),
                        getString(
                            R.string.transaction_success_message,
                            it.amount,
                            getNetwork(it.network).token
                        )
                    )
                }
                stopPendingAccounts()
                clear()
                viewModel.clearAndUnsubscribe()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
    }

    private fun prepareObservers() {
        viewModel.apply {
            walletConnectStatus.observe(this@MainActivity, Observer {
                Timber.tag("kobe").d("main activity")
                if (it is OnEthSign){
                    AlertDialogHandler.showDialog(this@MainActivity, "title", it.message)
                }
            })
            notExistedIdentityLiveData.observe(this@MainActivity, EventObserver {
                Toast.makeText(this@MainActivity, getString(R.string.not_existed_identity_message), Toast.LENGTH_LONG)
                    .show()
            })
            requestedFieldsLiveData.observe(this@MainActivity, EventObserver {
                Toast.makeText(this@MainActivity, getString(R.string.fill_requested_data_message, it), Toast.LENGTH_LONG)
                    .show()
            })
            errorLiveData.observe(this@MainActivity, EventObserver {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.unexpected_error),
                    Toast.LENGTH_LONG
                ).show()
            })
            loadingLiveData.observe(this@MainActivity, EventObserver {
                (getCurrentFragment() as? AccountsFragment)?.setPendingAccount(it.first, it.second)
            })
            updateCredentialSuccessLiveData.observe(this@MainActivity, EventObserver {
                showBindCredentialFlashbar(true, it)
            })
            updateCredentialErrorLiveData.observe(this@MainActivity, EventObserver {
                var message: String? = null
                if (it is AutomaticBackupFailedThrowable) {
                    message = getString(R.string.automatic_backup_failed_error)
                }
                showBindCredentialFlashbar(false, message)
            })
            updatePendingAccountLiveData.observe(
                this@MainActivity,
                EventObserver { updatePendingAccount(it) })
            updatePendingTransactionErrorLiveData.observe(this@MainActivity, EventObserver {
                showFlashbar(
                    getString(R.string.error_header),
                    getString(R.string.pending_account_error_message)
                )
                stopPendingAccounts()
                viewModel.clearPendingAccounts()
            })

            handleTimeoutOnPendingTransactionsLiveData.observe(this@MainActivity, EventObserver {
                it.forEach { pendingAccount -> handlePendingAccountsResults(pendingAccount) }
                stopPendingAccounts()
            })
        }
    }

    private fun updatePendingAccount(it: PendingAccount) {
        showFlashbar(
            getString(R.string.transaction_success_title),
            getString(R.string.transaction_success_message, it.amount, getNetwork(it.network).token)
        )
        (getCurrentFragment() as? AccountsFragment)?.apply {
            updateAccountFragment() {
                setPendingAccount(it.index, false)
            }
        }
        viewModel.clearWebSocketSubscription()
    }

    private fun stopPendingAccounts() {
        (getCurrentFragment() as? AccountsFragment)?.apply { updateAccountFragment() { stopPendingTransactions() } }
    }

    private fun handlePendingAccountsResults(account: PendingAccount) {
        if (account.blockHash != null) {
            showFlashbar(
                getString(R.string.transaction_success_title),
                getString(
                    R.string.transaction_success_message,
                    account.amount,
                    getNetwork(account.network).token
                )
            )
        } else {
            showFlashbar(
                getString(R.string.transaction_error_title),
                getString(
                    R.string.transaction_error_details_message,
                    account.amount,
                    account.network
                )
            )
        }
    }

    private fun showFlashbar(title: String, message: String) {
        MinervaFlashbar.show(this@MainActivity, title, message)
    }

    private fun AccountsFragment.updateAccountFragment(updatePending: () -> Unit) {
        updatePending()
        refreshBalances()
    }

    private fun prepareSettingsIcon() {
        if (!viewModel.isMnemonicRemembered()) binding.bottomNavigation.getOrCreateBadge(R.id.settings)
        else removeSettingsBadgeIcon()
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
                findItem(R.id.addIdentity)?.isVisible =
                    shouldShowAddIdentityIcon() && getCurrentChildFragment() is MyIdentitiesFragment
                findItem(R.id.editAccountOrder)?.isVisible =
                    shouldShowAddValueIcon() && isOrderEditAvailable(WalletActionType.ACCOUNT)
                findItem(R.id.addAccount)?.apply {
                    isVisible = shouldShowAddValueIcon()
                    isEnabled = !shouldDisableAddButton
                }
                findItem(R.id.editServiceOrder)?.isVisible =
                    isServicesTabSelected() && isOrderEditAvailable(WalletActionType.SERVICE)
                findItem(R.id.editCredentialsOrder)?.isVisible = isIdentitiesTabSelected() &&
                        getCurrentChildFragment() is CredentialsFragment && isOrderEditAvailable(
                    WalletActionType.CREDENTIAL
                )
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
            R.id.qrCodeScanner -> launchActivityForResult<LoginScannerActivity>(
                LOGIN_SCANNER_RESULT_REQUEST_CODE
            )
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
            val index = getIntExtra(ACCOUNT_INDEX, Int.InvalidValue)
            if (index != Int.InvalidValue) {
                viewModel.subscribeToExecutedTransactions(index)
                (getCurrentFragment() as? AccountsFragment)?.setPendingAccount(index, true)
            } else {
                getStringExtra(TRANSACTION_MESSAGE)?.let {
                    showFlashbar(getString(R.string.transaction_success_title), it)
                }
            }
        }

    }

    override fun showTransactionScreen(index: Int, assetIndex: Int, screenIndex: Int) {
        launchActivityForResult<TransactionActivity>(TRANSACTION_RESULT_REQUEST_CODE) {
            putExtra(ACCOUNT_INDEX, index)
            putExtra(ASSET_INDEX, assetIndex)
            putExtra(TRANSACTION_SCREEN, screenIndex)
        }
    }

    override fun shouldShowLoadingScreen(isLoading: Boolean) {
        binding.loadingScreen.apply {
            visibleOrGone(isLoading)
            startAnimation()
        }
        toggleLoadingActionBar(isLoading)
        toggleAddValueButton(isLoading)
    }

    private fun toggleLoadingActionBar(isLoading: Boolean) {
        if (isLoading) changeActionBarColor(R.color.loadingScreenBackground)
        else changeActionBarColor(R.color.lightGray)
    }

    private fun toggleAddValueButton(isLoading: Boolean) {
        shouldDisableAddButton = isLoading
        invalidateOptionsMenu()
    }

    override fun changeActionBarColor(color: Int) {
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(color)))
        window.statusBarColor = getColor(color)
    }

    override fun onBackPressed() {
        if (isIdentitiesTabSelected()) super.onBackPressed()
        else binding.bottomNavigation.selectedItemId = R.id.identities
    }

    override fun removeSettingsBadgeIcon() =
        binding.bottomNavigation.removeBadge(R.id.settings)

    override fun showWalletConnectScanner(index: Int) {
        launchActivity<WalletConnectActivity>() {
            putExtra(ACCOUNT_INDEX, index)
        }
    }

    private fun startNewAccountActivity() {
        startNewAccountWrappedActivity(
            this,
            String.format(
                NEW_VALUE_TITLE_PATTERN,
                getString(R.string.new_account),
                viewModel.getValueIterator()
            )
        )
    }

    fun onPainlessLogin() {
        viewModel.painlessLogin()
    }

    fun onAllowNotifications(shouldLogin: Boolean) {
        if (shouldLogin) viewModel.painlessLogin()
        else Toast.makeText(
            this,
            "Allow push notifications, will be added soon",
            Toast.LENGTH_SHORT
        ).show()
        //        TODO send to API that push notifications are allowed
    }

    fun onDenyNotifications() {
//        TODO send to API that push notifications are no longer allowed
        Toast.makeText(this, "Disable push notifications, will be added soon", Toast.LENGTH_SHORT)
            .show()
    }

    companion object {
        private const val NEW_VALUE_TITLE_PATTERN = "%s #%d"
        const val LOGIN_SCANNER_RESULT_REQUEST_CODE = 3
        const val TRANSACTION_RESULT_REQUEST_CODE = 4
        const val EDIT_IDENTITY_RESULT_REQUEST_CODE = 5
        const val JWT = "jwt"
        const val ACCOUNT_INDEX = "account_index"
    }
}