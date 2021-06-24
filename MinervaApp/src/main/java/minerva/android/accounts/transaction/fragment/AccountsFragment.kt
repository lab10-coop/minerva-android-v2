package minerva.android.accounts.transaction.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import minerva.android.R
import minerva.android.accounts.adapter.AccountAdapter
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.databinding.RefreshableRecyclerViewLayoutBinding
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.showBiometricPrompt
import minerva.android.kotlinUtils.event.Event
import minerva.android.main.base.BaseFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.dialog.EditAccountNameDialog
import minerva.android.widget.dialog.ExportPrivateKeyDialog
import minerva.android.widget.dialog.SelectPredefinedAccountDialog
import minerva.android.widget.state.AccountWidgetState
import minerva.android.widget.state.AppUIState
import minerva.android.wrapped.startManageTokensWrappedActivity
import minerva.android.wrapped.startRampWrappedActivity
import minerva.android.wrapped.startSafeAccountWrappedActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AccountsFragment : BaseFragment(R.layout.refreshable_recycler_view_layout), AccountsFragmentToAdapterListener {

    private val viewModel: AccountsViewModel by viewModel()
    private val appUIState: AppUIState by inject()
    private val accountAdapter by lazy { AccountAdapter(this) }

    private lateinit var binding: RefreshableRecyclerViewLayoutBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = RefreshableRecyclerViewLayoutBinding.bind(view)
        initFragment()
        setupRecycleView(view)
        setupLiveData()
    }

    override fun onResume() {
        super.onResume()
        interactor.changeActionBarColor(R.color.lightGray)
        viewModel.apply {
            onResume()
            refreshAddCryptoButton()
            if (arePendingAccountsEmpty) accountAdapter.stopPendingTransactions()
        }
    }

    fun stopPendingTransactions() = accountAdapter.stopPendingTransactions()

    override fun onSendTransaction(index: Int) = interactor.showTransactionScreen(index)

    override fun onSendTokenTransaction(accountIndex: Int, tokenAddress: String) =
        interactor.showTransactionScreen(accountIndex, tokenAddress)

    override fun onCreateSafeAccount(account: Account) = viewModel.createSafeAccount(account)

    override fun onAccountHide(account: Account) =
        AlertDialogHandler.showHideAccountDialog(
            requireContext(),
            getString(R.string.hide_account_dialog_title),
            getString(R.string.hide_account_dialog_message)
        ) { viewModel.hideAccount(account) }

    override fun onEditName(account: Account) = EditAccountNameDialog(requireContext(), account, ::changeAccountName).show()

    private fun changeAccountName(account: Account, newName: String) {
        viewModel.changeAccountName(account, newName)
    }

    override fun onShowAddress(accountIndex: Int) =
        interactor.showTransactionScreen(accountIndex, screenIndex = RECEIVE_TRANSACTION_INDEX)

    override fun onShowSafeAccountSettings(account: Account, position: Int) =
        startSafeAccountWrappedActivity(
            requireContext(),
            account.name,
            position,
            account.network.chainId,
            account.isSafeAccount
        )

    override fun onWalletConnect(index: Int) = interactor.showWalletConnectScanner(index)

    override fun onManageTokens(index: Int) = startManageTokensWrappedActivity(requireContext(), index)

    override fun onExportPrivateKey(account: Account) =
        if (viewModel.isProtectKeysEnabled) showBiometricPrompt({ showExportDialog(account) })
        else showExportDialog(account)

    private fun showExportDialog(account: Account) = ExportPrivateKeyDialog(requireContext(), account).show()

    override fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState) =
        appUIState.updateAccountWidgetState(index, accountWidgetState)

    override fun getAccountWidgetState(index: Int): AccountWidgetState = appUIState.getAccountWidgetState(index)

    override fun getTokens(account: Account): List<ERC20Token> = viewModel.getTokens(account)

    fun setPendingAccount(index: Int, pending: Boolean) {
        accountAdapter.setPending(index, pending, viewModel.areMainNetsEnabled)
    }

    fun updateTokensRate() = viewModel.updateTokensRate()

    fun refreshBalances() = viewModel.refreshCoinBalances()

    private fun initFragment() {
        binding.apply {
            viewModel.apply {
                syncError.isGone = isSynced
                networksHeader.text = getHeader(areMainNetsEnabled)
                addCryptoButton.apply { text = getBuyCryptoButtonText(this) }
                if (isFirstLaunch) {
                    SelectPredefinedAccountDialog(requireContext(), ::createAccountForSelectedNetwork).show()
                }
            }
        }
    }

    private fun getBuyCryptoButtonText(materialButton: MaterialButton): String =
        if (viewModel.areMainNetsEnabled) {
            materialButton.setBackgroundColor(ContextCompat.getColor(materialButton.context, R.color.colorPrimary))
            getString(R.string.buy_crypto)
        } else {
            materialButton.setBackgroundColor(ContextCompat.getColor(materialButton.context, R.color.artis))
            getString(R.string.add_tats)
        }

    private fun setupRecycleView(view: View) {
        binding.apply {
            swipeRefresh.apply {
                setColorSchemeResources(
                    R.color.colorSetOne,
                    R.color.colorSetFour,
                    R.color.colorSetSeven,
                    R.color.colorSetNine
                )
                setOnRefreshListener {
                    with(viewModel) {
                        refreshCoinBalances()
                        refreshTokensBalances()
                        discoverNewTokens()
                    }
                }
            }

            recyclerView.apply {
                layoutManager = LinearLayoutManager(view.context)
                adapter = accountAdapter
            }
        }
    }

    private fun checkSwipe() = binding.swipeRefresh.run {
        if (isRefreshing) isRefreshing = !viewModel.isRefreshDone
    }

    private fun setupLiveData() {
        viewModel.apply {
            binding.apply {
                accountsLiveData.observe(viewLifecycleOwner, ObserverWithSyncChecking { accounts ->
                    noDataMessage.visibleOrGone(hasAvailableAccounts)
                    accountAdapter.setAccounts(accounts, activeAccounts, fiatSymbol)
                    setTatsButtonListener()
                })

                dappSessions.observe(viewLifecycleOwner, ObserverWithSyncChecking {
                    accountAdapter.updateSessionCount(it)
                })

                balanceLiveData.observe(viewLifecycleOwner, ObserverWithSyncChecking {
                    accountAdapter.updateBalances(it)
                    checkSwipe()
                })
                tokenBalanceLiveData.observe(viewLifecycleOwner, ObserverWithSyncChecking {
                    accountAdapter.updateTokenBalances()
                    checkSwipe()
                })
            }

            errorLiveData.observe(viewLifecycleOwner, EventObserverWithSyncChecking { errorState ->
                when (errorState) {
                    BaseError -> {
                        refreshAddCryptoButton()
                        showErrorFlashbar(R.string.error_header, R.string.unexpected_error)
                    }
                    BalanceIsNotEmptyAndHasMoreOwnersError ->
                        showErrorFlashbar(
                            R.string.cannot_remove_safe_account_title,
                            R.string.cannot_remove_safe_account_message
                        )
                    BalanceIsNotEmptyError ->
                        showErrorFlashbar(R.string.cannot_remove_account_title, R.string.cannot_remove_account_message)
                    IsNotSafeAccountMasterOwnerError -> showErrorFlashbar(
                        R.string.error_header,
                        R.string.safe_account_removal_error
                    )
                    is AutomaticBackupError -> handleAutomaticBackupError(errorState.throwable)
                    RefreshCoinBalancesError -> handleRefreshBalancesError(R.string.refresh_balances_error)
                    RefreshTokenBalancesError -> handleRefreshBalancesError(R.string.refresh_asset_balances_error)
                    NoFunds -> MinervaFlashbar.show(
                        requireActivity(),
                        getString(R.string.no_funds),
                        getString(R.string.no_funds_message)
                    )
                }
            })

            loadingLiveData.observe(viewLifecycleOwner, EventObserverWithSyncChecking {
                interactor.shouldShowLoadingScreen(it)
            })

            accountHideLiveData.observe(viewLifecycleOwner, EventObserverWithSyncChecking {
                activity?.invalidateOptionsMenu()
                refreshAddCryptoButton()
            })

            addFreeAtsLiveData.observe(viewLifecycleOwner, EventObserverWithSyncChecking { success ->
                (if (success) R.string.refresh_balance_to_check_transaction_status
                else R.string.free_ats_warning).apply {
                    Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun refreshAddCryptoButton() {
        viewModel.apply {
            isAddingFreeATSAvailable(activeAccounts).let { isAvailable ->
                binding.addCryptoButton.apply {
                    if (!viewModel.areMainNetsEnabled) {
                        val color = if (isAvailable) R.color.artis else R.color.inactiveButtonColor
                        setBackgroundColor(ContextCompat.getColor(context, color))
                    }
                }
            }
        }
    }

    private fun setTatsButtonListener() =
        binding.addCryptoButton.setOnClickListener { view ->
            viewModel.apply {
                if (areMainNetsEnabled) startRampWrappedActivity(requireContext())
                else {
                    view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.inactiveButtonColor))
                    addAtsToken()
                }
            }
        }

    private fun handleRefreshBalancesError(messageRes: Int) {
        binding.swipeRefresh.isRefreshing = false
        showErrorFlashbar(R.string.error_header, messageRes)
    }

    private fun showErrorFlashbar(titleRes: Int, messageRes: Int) =
        MinervaFlashbar.show(requireActivity(), getString(titleRes), getString(messageRes))

    private fun createAccountForSelectedNetwork(chainId: Int) {
        viewModel.createNewAccount(chainId)
    }

    private inner class EventObserverWithSyncChecking<T>(private val onEventUnhandledContent: (T) -> Unit) :
        Observer<Event<T>> {
        override fun onChanged(event: Event<T>?) {
            event?.getContentIfNotHandled()?.let {
                binding.syncError.isGone = viewModel.isSynced
                onEventUnhandledContent(it)
            }
        }
    }

    private inner class ObserverWithSyncChecking<T>(private val onValueChanged: (T) -> Unit) : Observer<T> {
        override fun onChanged(value: T) {
            binding.syncError.isGone = viewModel.isSynced
            onValueChanged(value)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AccountsFragment()
        private const val RECEIVE_TRANSACTION_INDEX = 1
    }
}
