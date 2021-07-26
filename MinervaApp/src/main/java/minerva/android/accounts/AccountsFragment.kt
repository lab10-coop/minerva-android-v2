package minerva.android.accounts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.button.MaterialButton
import minerva.android.R
import minerva.android.accounts.adapter.AccountAdapter
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.accounts.state.*
import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.databinding.RefreshableRecyclerViewLayoutBinding
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.showBiometricPrompt
import minerva.android.kotlinUtils.event.Event
import minerva.android.main.base.BaseFragment
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.dialog.EditAccountNameDialog
import minerva.android.widget.dialog.ExportPrivateKeyDialog
import minerva.android.widget.dialog.HideAccountDialog
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
        if (!appUIState.shouldShowSplashScreen) {
            interactor.changeActionBarColor(R.color.lightGray)
            viewModel.apply {
                onResume()
                refreshAddCryptoButton()
                if (arePendingAccountsEmpty) accountAdapter.stopPendingTransactions()
            }
        }
    }

    fun stopPendingTransactions() = accountAdapter.stopPendingTransactions()

    override fun onSendTransaction(account: Account) =
        interactor.showTransactionScreen(viewModel.indexOfRawAccounts(account), isCoinBalanceError = account.isError)

    override fun onSendTokenTransaction(account: Account, tokenAddress: String, isTokenError: Boolean) {
        interactor.showTransactionScreen(
            viewModel.indexOfRawAccounts(account),
            tokenAddress,
            isTokenBalanceError = isTokenError
        )
    }

    override fun onCreateSafeAccount(account: Account) = viewModel.createSafeAccount(account)

    override fun onAccountHide(index: Int) =
        HideAccountDialog(requireContext(), index, ::hideAccount).show()

    private fun hideAccount(index: Int) = viewModel.hideAccount(index)

    override fun onEditName(account: Account) =
        EditAccountNameDialog(requireContext(), account, ::changeAccountName).show()

    override fun updateSessionCount(
        sessionsPerAccount: List<DappSessionData>,
        passIndex: (index: Int) -> Unit
    ) {
        viewModel.updateSessionCount(sessionsPerAccount, passIndex)
    }

    override fun showPendingAccount(
        index: Int,
        chainId: Int,
        areMainNetsEnabled: Boolean,
        isPending: Boolean,
        passIndex: (index: Int) -> Unit
    ) {
        viewModel.showPendingAccount(index, chainId, areMainNetsEnabled, isPending, passIndex)
    }

    override fun indexOf(account: Account): Int = viewModel.indexOfRawAccounts(account)
    override fun stopPendingAccounts() {
        viewModel.stopPendingAccounts()
    }

    private fun changeAccountName(account: Account, newName: String) {
        viewModel.changeAccountName(account, newName)
    }

    override fun onShowAddress(account: Account) =
        interactor.showTransactionScreen(viewModel.indexOfRawAccounts(account), screenIndex = RECEIVE_TRANSACTION_INDEX)

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

    override fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState) =
        appUIState.updateAccountWidgetState(index, accountWidgetState)

    override fun getAccountWidgetState(index: Int): AccountWidgetState = appUIState.getAccountWidgetState(index)
    override fun getTokens(account: Account): List<ERC20Token> = viewModel.getTokens(account)
    fun updateTokensRate() = viewModel.updateTokensRate()
    fun refreshBalances() = viewModel.refreshCoinBalances()
    fun setPendingAccount(index: Int, chainId: Int, pending: Boolean) {
        accountAdapter.setPending(index, chainId, pending, viewModel.areMainNetsEnabled)
    }

    private fun showExportDialog(account: Account) = ExportPrivateKeyDialog(requireContext(), account).show()


    private fun initFragment() {
        binding.apply {
            viewModel.apply {
                syncError.isGone = isSynced
                networksHeader.text = getHeader(areMainNetsEnabled)
                addCryptoButton.apply { text = getBuyCryptoButtonText(this) }
                if (!appUIState.shouldShowSplashScreen && isFirstLaunch) {
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
                (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }
        }
    }

    private fun checkSwipe() = binding.swipeRefresh.run {
        if (isRefreshing) isRefreshing = !viewModel.isRefreshDone
    }

    private fun setupLiveData() {
        viewModel.apply {
            binding.apply {
                accountsLiveData.observe(viewLifecycleOwner, ObserverWithSyncChecking { _ ->
                    noDataMessage.visibleOrGone(hasAvailableAccounts)
                    accountAdapter.submitList(activeAccounts)
                    accountAdapter.setFiat(fiatSymbol)
                    setTatsButtonListener()
                })

                dappSessions.observe(viewLifecycleOwner, ObserverWithSyncChecking { sessionsPerAccount ->
                    accountAdapter.updateSessionCount(sessionsPerAccount)
                })

                balanceStateLiveData.observe(viewLifecycleOwner, ObserverWithSyncChecking { state ->
                    when (state) {
                        is CoinBalanceState -> updateCoinBalanceState(state)
                        is TokenBalanceState -> updateTokenBalanceState(state)
                        is UpdateAllState -> accountAdapter.refreshList()
                    }
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
                    RefreshBalanceError -> binding.swipeRefresh.isRefreshing = false
                    NoFunds -> MinervaFlashbar.show(
                        requireActivity(),
                        getString(R.string.no_funds),
                        getString(R.string.no_funds_message)
                    )
                }
            })

            loadingLiveData.observe(viewLifecycleOwner, EventObserverWithSyncChecking { isVisible ->
                interactor.shouldShowLoadingScreen(isVisible)
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

    private fun updateCoinBalanceState(state: CoinBalanceState) {
        when (state) {
            is CoinBalanceUpdate -> accountAdapter.updateCoinBalance(state.index)
            is CoinBalanceCompleted -> checkSwipe()
        }
    }

    private fun updateTokenBalanceState(state: TokenBalanceState) {
        when (state) {
            is TokenBalanceUpdate -> accountAdapter.updateTokenBalance(state.index)
            is TokenBalanceCompleted -> checkSwipe()
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
