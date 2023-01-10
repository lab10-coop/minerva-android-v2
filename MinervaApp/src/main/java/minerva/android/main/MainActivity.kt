package minerva.android.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import minerva.android.R
import minerva.android.accounts.AccountsFragment
import minerva.android.accounts.nft.view.NftCollectionActivity
import minerva.android.accounts.nft.view.NftCollectionActivity.Companion.COLLECTION_NAME
import minerva.android.accounts.transaction.activity.TransactionActivity
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.COIN_BALANCE_ERROR
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.TOKEN_BALANCE_ERROR
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.TRANSACTION_MESSAGE
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.TRANSACTION_SCREEN
import minerva.android.accounts.walletconnect.*
import minerva.android.databinding.ActivityMainBinding
import minerva.android.extension.*
import minerva.android.identities.IdentitiesFragment
import minerva.android.identities.credentials.CredentialsFragment
import minerva.android.identities.myIdentities.MyIdentitiesFragment
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.base.BaseFragment
import minerva.android.main.error.*
import minerva.android.main.handler.*
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.services.login.ServicesScannerActivity
import minerva.android.splash.SplashScreenActivity
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletConnect.BaseWalletConnectInteractionsActivity
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.manager.networks.NetworkManager.getNetwork
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.wrapped.*
import org.koin.android.ext.android.inject
import java.net.ConnectException

class MainActivity : BaseWalletConnectInteractionsActivity(), FragmentInteractorListener {

    internal val viewModel: MainViewModel by inject()
    private var shouldDisableAddButton = false
    internal lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeSplashScreenRedirection()
        if (!viewModel.shouldShowSplashScreen()) {
            prepareBottomNavMenu()
            replaceFragment(AccountsFragment.newInstance())
            prepareSettingsIcon()
            prepareObservers()
            handleOutdatedWalletErrorDialog()
            with(viewModel) {
                restorePendingTransactions()
                checkMissingTokensDetails()
                discoverNewTokens()
            }
        }
    }

    private fun handleOutdatedWalletErrorDialog() {
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
        if (!viewModel.shouldShowSplashScreen()) {
            shouldShowLoadingScreen(false)
            handleExecutedAccounts()
            viewModel.getTokensRate()
        }
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
                            getNetwork(it.chainId).token
                        )
                    )
                }
                stopPendingAccounts()
                clear()
                viewModel.clearAndUnsubscribe()
            }
        }
    }

    override fun isProtectTransactionEnabled(): Boolean = viewModel.isProtectTransactionEnabled()

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
    }

    private fun prepareObservers() {
        prepareWalletConnect()
        viewModel.apply {
            errorLiveData.observe(this@MainActivity, EventObserver { errorStatus ->
                when (errorStatus) {
                    is UpdateCredentialError -> handleUpdateCredentialError(errorStatus.throwable)
                    is RequestedFields -> showToast(
                        getString(
                            R.string.fill_requested_data_message,
                            errorStatus.identityName
                        )
                    )
                    UpdatePendingTransactionError -> handleUpdatePendingTransactionError()
                    BaseError -> showToast(getString(R.string.unexpected_error))
                    NotExistedIdentity -> showToast(getString(R.string.not_existed_identity_message))
                }
            })
            updateCredentialSuccessLiveData.observe(this@MainActivity, EventObserver {
                showBindCredentialFlashbar(true, it)
            })
            updatePendingAccountLiveData.observe(this@MainActivity, EventObserver { updatePendingAccount(it) })
            handleTimeoutOnPendingTransactionsLiveData.observe(this@MainActivity, EventObserver { pendingAccounts ->
                pendingAccounts.forEach { pendingAccount -> handlePendingAccountsResults(pendingAccount) }
                stopPendingAccounts()
            })
            updateTokensRateLiveData.observe(this@MainActivity, EventObserver {
                (getCurrentFragment() as? AccountsFragment)?.updateTokensRate()
            })
        }
    }

    private fun observeSplashScreenRedirection() {
        viewModel.redirectToSplashScreenLiveData.observe(this@MainActivity, EventObserver {
            launchActivity<SplashScreenActivity> { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun handleUpdateCredentialError(throwable: Throwable) {
        var message: String? = null
        if (throwable is AutomaticBackupFailedThrowable) {
            message = getString(R.string.automatic_backup_failed_error)
        }
        showBindCredentialFlashbar(false, message)
    }

    private fun handleUpdatePendingTransactionError() {
        showFlashbar(
            getString(R.string.error_header),
            getString(R.string.pending_account_error_message)
        )
        stopPendingAccounts()
        viewModel.clearPendingAccounts()
    }

    private fun updatePendingAccount(account: PendingAccount) {
        showFlashbar(
            getString(R.string.transaction_success_title),
            getString(R.string.transaction_success_message, account.amount, getNetwork(account.chainId).token)
        )
        (getCurrentFragment() as? AccountsFragment)?.apply {
            updateAccountFragment { setPendingAccount(account.index, account.chainId, false) }
        }
        viewModel.clearWebSocketSubscription()
    }

    private fun stopPendingAccounts() {
        (getCurrentFragment() as? AccountsFragment)?.apply { updateAccountFragment { stopPendingTransactions() } }
    }

    private fun handlePendingAccountsResults(account: PendingAccount) {
        if (account.blockHash != null) {
            showFlashbar(
                getString(R.string.transaction_success_title),
                getString(
                    R.string.transaction_success_message,
                    account.amount,
                    getNetwork(account.chainId).token
                )
            )
        } else {
            showFlashbar(
                getString(R.string.transaction_error_title),
                getString(
                    R.string.transaction_error_details_message,
                    account.amount,
                    account.chainId
                )
            )
        }
    }

    private fun AccountsFragment.updateAccountFragment(updatePending: () -> Unit) {
        updatePending()
        refreshBalances()
    }

    private fun prepareSettingsIcon() {
        if (!viewModel.isMnemonicRemembered()) binding.bottomNavigation.getOrCreateBadge(R.id.settings)
        else removeSettingsBadgeIcon()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    @SuppressLint("WrongConstant")
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
            R.id.qrCodeScanner -> launchActivityForResult<ServicesScannerActivity>(SERVICES_SCANNER_RESULT_REQUEST_CODE)
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
                val chainId = getIntExtra(ACCOUNT_CHAIN_ID, Int.InvalidValue)
                try {
                    viewModel.subscribeToExecutedTransactions(index)
                    (getCurrentFragment() as? AccountsFragment)?.setPendingAccount(index, chainId, true)
                } catch (e: ConnectException) {
                    FirebaseCrashlytics.getInstance()
                        .recordException(Throwable("Native Coin Send: Failed to ws connect: ${chainId}"))
                    getStringExtra(TRANSACTION_MESSAGE)?.let { message ->
                        showFlashbar(getString(R.string.transaction_success_title), message)
                    }
                }
            } else {
                getStringExtra(TRANSACTION_MESSAGE)?.let { message ->
                    showFlashbar(getString(R.string.transaction_success_title), message)
                }
            }
        }
    }

    override fun showTransactionScreen(
        index: Int,
        tokenAddress: String,
        screenIndex: Int,
        isCoinBalanceError: Boolean,
        isTokenBalanceError: Boolean
    ) {
        launchActivityForResult<TransactionActivity>(TRANSACTION_RESULT_REQUEST_CODE) {
            putExtra(ACCOUNT_INDEX, index)
            putExtra(TOKEN_ADDRESS, tokenAddress)
            putExtra(TRANSACTION_SCREEN, screenIndex)
            putExtra(COIN_BALANCE_ERROR, isCoinBalanceError)
            putExtra(TOKEN_BALANCE_ERROR, isTokenBalanceError)
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
        if (isValuesTabSelected()) super.onBackPressed()
        else setDefaultBottomNavigationIcon()
    }

    override fun removeSettingsBadgeIcon() =
        binding.bottomNavigation.removeBadge(R.id.settings)

    override fun showWalletConnectScanner(index: Int) {
        // todo: maybe there should be a values screen scanner result request code?
        launchActivityForResult<ServicesScannerActivity>(SERVICES_SCANNER_RESULT_REQUEST_CODE)
    }

    override fun showNftCollectionScreen(index: Int, tokenAddress: String, collectionName: String, isGroup: Boolean) {
        launchActivity<NftCollectionActivity> {
            putExtra(ACCOUNT_INDEX, index)
            putExtra(TOKEN_ADDRESS, tokenAddress)
            putExtra(COLLECTION_NAME, collectionName)
            putExtra(IS_GROUP, isGroup)
        }
    }

    private fun setDefaultBottomNavigationIcon() {
        binding.bottomNavigation.selectedItemId = R.id.values
    }

    private fun startNewAccountActivity() {
        startNewAccountWrappedActivity(
            this,
            getString(R.string.add_account)
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
        const val ZERO = 0
        const val ONE = 1
        const val SERVICES_SCANNER_RESULT_REQUEST_CODE = 3
        const val TRANSACTION_RESULT_REQUEST_CODE = 4
        const val EDIT_IDENTITY_RESULT_REQUEST_CODE = 5
        const val JWT = "jwt"
        const val TOKEN_ADDRESS = "token_address"
        const val ACCOUNT_INDEX = "account_index"
        const val ACCOUNT_CHAIN_ID = "account_indicator"
        const val IS_GROUP = "ig_group"
    }
}