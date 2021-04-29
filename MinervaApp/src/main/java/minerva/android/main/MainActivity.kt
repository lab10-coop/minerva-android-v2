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
import minerva.android.R
import minerva.android.accounts.transaction.activity.TransactionActivity
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.TOKEN_ADDRESS
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.TRANSACTION_MESSAGE
import minerva.android.accounts.transaction.activity.TransactionActivity.Companion.TRANSACTION_SCREEN
import minerva.android.accounts.transaction.fragment.AccountsFragment
import minerva.android.accounts.walletconnect.*
import minerva.android.databinding.ActivityMainBinding
import minerva.android.extension.*
import minerva.android.extensions.showBiometricPrompt
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
import minerva.android.main.walletconnect.WalletConnectInteractionsViewModel
import minerva.android.services.login.LoginScannerActivity
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.WalletConnectConnectionThrowable
import minerva.android.walletmanager.manager.accounts.AccountManagerImpl.Companion.NEW_ACCOUNT_TITLE_PATTERN
import minerva.android.walletmanager.manager.networks.NetworkManager.getNetwork
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.dialog.MinervaLoadingDialog
import minerva.android.widget.dialog.walletconnect.DappDialog
import minerva.android.widget.dialog.walletconnect.DappSendTransactionDialog
import minerva.android.widget.dialog.walletconnect.DappSignMessageDialog
import minerva.android.widget.dialog.walletconnect.GasPriceDialog
import minerva.android.wrapped.*
import org.koin.android.ext.android.inject
import java.math.BigDecimal

class MainActivity : AppCompatActivity(), FragmentInteractorListener {

    internal val viewModel: MainViewModel by inject()
    private val walletConnectViewModel: WalletConnectInteractionsViewModel by inject()
    private var shouldDisableAddButton = false
    internal lateinit var binding: ActivityMainBinding
    private var dappDialog: DappDialog? = null
    private var loadingDialog: MinervaLoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prepareBottomNavMenu()
        replaceFragment(AccountsFragment.newInstance())
        prepareSettingsIcon()
        prepareObservers()
        if (!viewModel.isBackupAllowed) {
            AlertDialogHandler.showDialog(
                this,
                getString(R.string.error_header),
                getString(R.string.outdated_wallet_error_message)
            )
        }

        //todo move to viewModel init ??
        with(viewModel) {
            restorePendingTransactions()
            checkMissingTokensDetails()
        }
    }

    override fun onResume() {
        super.onResume()
        shouldShowLoadingScreen(false)
        handleExecutedAccounts()
        viewModel.getTokensRate()
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

    override fun onPause() {
        super.onPause()
        rejectRequest()
    }

    private fun rejectRequest() {
        dappDialog?.let {
            it.dismiss()
            walletConnectViewModel.rejectRequest()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.dispose()
        walletConnectViewModel.dispose()
    }

    private fun prepareObservers() {
        walletConnectViewModel.walletConnectStatus.observe(this@MainActivity, Observer {
            dappDialog?.dismiss()
            when (it) {
                is OnEthSignRequest -> dappDialog = getDappSignDialog(it)
                is OnEthSendTransactionRequest -> dappDialog = getSendTransactionDialog(it)
                is ProgressBarState -> handleLoadingDialog(it)
                is OnError -> handleWalletConnectError(it.error)
                else -> dappDialog = null
            }
        })
        walletConnectViewModel.errorLiveData.observe(this, EventObserver {
            handleWalletConnectError(it)
        })
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
            loadingLiveData.observe(this@MainActivity, EventObserver {
                (getCurrentFragment() as? AccountsFragment)?.setPendingAccount(it.first, it.second)
            })
            updateCredentialSuccessLiveData.observe(this@MainActivity, EventObserver {
                showBindCredentialFlashbar(true, it)
            })
            updatePendingAccountLiveData.observe(
                this@MainActivity,
                EventObserver { updatePendingAccount(it) })
            handleTimeoutOnPendingTransactionsLiveData.observe(this@MainActivity, EventObserver {
                it.forEach { pendingAccount -> handlePendingAccountsResults(pendingAccount) }
                stopPendingAccounts()
            })
            updateTokensRateLiveData.observe(this@MainActivity, EventObserver {
                (getCurrentFragment() as? AccountsFragment)?.updateTokensRate()
            })
        }
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

    private fun handleWalletConnectError(error: Throwable) {
        if (error is WalletConnectConnectionThrowable) {
            dappDialog?.dismiss()
            showFlashbar(getString(R.string.wallet_connect_title), getString(R.string.wc_connection_error_message))
        }
    }

    private fun handleLoadingDialog(it: ProgressBarState) {
        if (it.show) {
            loadingDialog = MinervaLoadingDialog(this).apply { show() }
        } else {
            loadingDialog?.dismiss()
        }
    }

    private fun getSendTransactionDialog(it: OnEthSendTransactionRequest) =
        DappSendTransactionDialog(
            this,
            {
                if (viewModel.isProtectTransactionEabled()) getCurrentFragment()?.showBiometricPrompt ({ walletConnectViewModel.sendTransaction() })
                else walletConnectViewModel.sendTransaction()
                dappDialog = null
            },
            {
                walletConnectViewModel.rejectRequest()
                dappDialog = null
            }).apply {
            setContent(
                it.transaction, it.session, it.account,
                { showGasPriceDialog(it) },
                { gasPrice -> walletConnectViewModel.recalculateTxCost(gasPrice, it.transaction) },
                { balance, cost -> walletConnectViewModel.isBalanceTooLow(balance, cost) }

            )
            show()
        }

    private fun DappSendTransactionDialog.showGasPriceDialog(it: OnEthSendTransactionRequest) {
        GasPriceDialog(context) { gasPrice ->
            setCustomGasPrice(
                walletConnectViewModel.recalculateTxCost(BigDecimal(gasPrice), it.transaction),
                it.account
            ) { balance, cost -> walletConnectViewModel.isBalanceTooLow(balance, cost) }
        }.show()
    }

    private fun getDappSignDialog(it: OnEthSignRequest) =
        DappSignMessageDialog(this@MainActivity, {
            walletConnectViewModel.acceptRequest()
            dappDialog = null
        },
            {
                walletConnectViewModel.rejectRequest()
                dappDialog = null
            }
        ).apply {
            setContent(it.message, it.session)
            show()
        }

    private fun updatePendingAccount(it: PendingAccount) {
        showFlashbar(
            getString(R.string.transaction_success_title),
            getString(R.string.transaction_success_message, it.amount, getNetwork(it.chainId).token)
        )
        (getCurrentFragment() as? AccountsFragment)?.apply {
            updateAccountFragment { setPendingAccount(it.index, false) }
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

    override fun showTransactionScreen(index: Int, tokenAddress: String, screenIndex: Int) {
        launchActivityForResult<TransactionActivity>(TRANSACTION_RESULT_REQUEST_CODE) {
            putExtra(ACCOUNT_INDEX, index)
            putExtra(TOKEN_ADDRESS, tokenAddress)
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
        if (isValuesTabSelected()) super.onBackPressed()
        else setDefaultBottomNavigationIcon()
    }

    override fun removeSettingsBadgeIcon() =
        binding.bottomNavigation.removeBadge(R.id.settings)

    override fun showWalletConnectScanner(index: Int) {
        launchActivity<WalletConnectActivity> {
            putExtra(ACCOUNT_INDEX, index)
        }
    }

    private fun setDefaultBottomNavigationIcon() {
        binding.bottomNavigation.selectedItemId = R.id.values
    }

    private fun startNewAccountActivity() {
        startNewAccountWrappedActivity(
            this,
            String.format(
                NEW_ACCOUNT_TITLE_PATTERN,
                getString(R.string.new_account),
                viewModel.getAccountIterator()
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
        const val LOGIN_SCANNER_RESULT_REQUEST_CODE = 3
        const val TRANSACTION_RESULT_REQUEST_CODE = 4
        const val EDIT_IDENTITY_RESULT_REQUEST_CODE = 5
        const val JWT = "jwt"
        const val ACCOUNT_INDEX = "account_index"
    }
}