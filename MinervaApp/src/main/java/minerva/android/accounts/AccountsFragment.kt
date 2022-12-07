package minerva.android.accounts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.firebase.iid.FirebaseInstanceId
import minerva.android.R
import minerva.android.accounts.adapter.AccountAdapter
import minerva.android.accounts.adapter.AccountViewHolder
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.accounts.state.*
import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.databinding.RefreshableRecyclerViewLayoutBinding
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.showBiometricPrompt
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.kotlinUtils.NO_MARGIN
import minerva.android.kotlinUtils.event.Event
import minerva.android.main.base.BaseFragment
import minerva.android.utils.VerticalMarginItemDecoration
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.dialog.*
import minerva.android.widget.state.AccountWidgetState
import minerva.android.widget.state.AppUIState
import minerva.android.wrapped.startManageTokensWrappedActivity
import minerva.android.wrapped.startNewAccountWrappedActivity
import minerva.android.wrapped.startRampWrappedActivity
import minerva.android.wrapped.startSafeAccountWrappedActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AccountsFragment : BaseFragment(R.layout.refreshable_recycler_view_layout),
    AccountsFragmentToAdapterListener {
    private val viewModel: AccountsViewModel by viewModel()
    private val appUIState: AppUIState by inject()
    private val logger: Logger by inject()
    private val accountAdapter: AccountAdapter by lazy { AccountAdapter(this) }
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
                if (arePendingAccountsEmpty) {
                    accountAdapter.stopPendingTransactions()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopStreaming()
        endSuperTokenStreamAnimation()
    }

    private fun endSuperTokenStreamAnimation() {
        for (i in Int.FirstIndex..accountAdapter.itemCount) {
            (binding.recyclerView.findViewHolderForAdapterPosition(i) as? AccountViewHolder)?.endStreamAnimation()
        }
    }

    fun stopPendingTransactions() = accountAdapter.stopPendingTransactions()

    override fun onSendTransaction(account: Account) {
        //show "ShowWarningDialog" if account/network has "Unmaintained Network" status
        if (!account.isActiveNetwork && account.showWarning) {
            ShowWarningDialog(requireContext(), true) { state: Boolean ->
                //prevent showing "ShowWarningDialog" dialog in future(set Account::showWarning to false)
                if (state) viewModel.changeShowWarning(account, !state)

                interactor.showTransactionScreen(
                    viewModel.indexOfRawAccounts(account),
                    isCoinBalanceError = account.isError
                )
            }.show()
        } else {
            interactor.showTransactionScreen(
                viewModel.indexOfRawAccounts(account),
                isCoinBalanceError = account.isError
            )
        }
    }

    override fun onSendTokenTransaction(
        account: Account,
        tokenAddress: String,
        isTokenError: Boolean
    ) {
        interactor.showTransactionScreen(
            viewModel.indexOfRawAccounts(account),
            tokenAddress,
            isTokenBalanceError = isTokenError
        )
    }

    override fun onNftCollectionClicked(account: Account, tokenAddress: String, collectionName: String, isGroup: Boolean) {
        interactor.showNftCollectionScreen(
            viewModel.indexOfRawAccounts(account),
            tokenAddress,
            collectionName,
            isGroup
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
        updateAccountsList()
    }

    private fun updateAccountsList(accounts: List<Account> = viewModel.activeAccounts) = accountAdapter.updateList(accounts)

    private fun changeAccountName(account: Account, newName: String) {
        viewModel.changeAccountName(account, newName)
    }

    override fun onShowAddress(account: Account) =
        interactor.showTransactionScreen(
            viewModel.indexOfRawAccounts(account),
            screenIndex = RECEIVE_TRANSACTION_INDEX
        )

    override fun onShowSafeAccountSettings(account: Account, position: Int) =
        startSafeAccountWrappedActivity(
            requireContext(),
            account.name,
            position,
            account.network.chainId,
            account.isSafeAccount
        )

    override fun onWalletConnect(index: Int) = interactor.showWalletConnectScanner(index)
    override fun onManageTokens(index: Int) =
        startManageTokensWrappedActivity(requireContext(), index)

    override fun onExportPrivateKey(account: Account) =
        if (viewModel.isProtectKeysEnabled) showBiometricPrompt({ showExportDialog(account) })
        else showExportDialog(account)

    override fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState) {
        appUIState.updateAccountWidgetState(index, accountWidgetState)
    }

    override fun getAccountWidgetState(index: Int): AccountWidgetState =
        appUIState.getAccountWidgetState(index)

    override fun openInExplorer(account: Account) {
        //check all necessary data is filled
        if (!account.network.explore.isEmpty() && !account.address.isEmpty()) {
            //build full web path to explore transaction
            val base = if (account.network.explore.endsWith('/')) account.network.explore else "${account.network.explore}/"
            val path = "${base}${ADDRESS}/${account.address}"
            //create web Intent
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(path))
            startActivity(browserIntent)
        }
    }

    override fun getTokens(account: Account): List<AccountToken> = viewModel.getTokens(account)
    fun updateTokensRate() = viewModel.updateTokensRate()
    fun refreshBalances() = viewModel.refreshCoinBalances()
    fun setPendingAccount(index: Int, chainId: Int, pending: Boolean) {
        accountAdapter.setPending(index, chainId, pending, viewModel.areMainNetsEnabled)
    }

    private fun showExportDialog(account: Account) =
        ExportPrivateKeyDialog(requireContext(), account).show()


    private fun initFragment() {
        binding.apply {
            viewModel.apply {
                logToFirebaseIfNotSynced()
                networksHeader.text = getHeader(areMainNetsEnabled)
                addCryptoButton.apply {
                    if (viewModel.areMainNetsEnabled) {
                        this.setBackgroundColor( ContextCompat.getColor(this.context, R.color.colorPrimary) )
                        text = getString(R.string.buy_crypto)
                    } else {
                        this.visibility = View.GONE
                    }
                }
                if (!appUIState.shouldShowSplashScreen && isFirstLaunch) {
                    SelectPredefinedAccountDialog(
                        requireContext(),
                        ::createAccountForSelectedNetwork
                    ).show()
                }
            }
        }
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
                        stopStreaming()
                        refreshCoinBalances()
                        refreshTokensBalances()
                        discoverNewTokens()
                        updateTokensRate()
                    }
                }
            }

            recyclerView.apply {
                layoutManager = LinearLayoutManager(view.context)
                adapter = accountAdapter
                (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(getRecyclerViewItemDecorator())
            }
        }
    }

    private fun getRecyclerViewItemDecorator(): VerticalMarginItemDecoration {
        val margin = requireContext().resources.getDimension(R.dimen.margin_xxsmall).toInt()
        val bottomMargin = requireContext().resources.getDimension(R.dimen.margin_small).toInt()
        return VerticalMarginItemDecoration(margin, Int.NO_MARGIN, bottomMargin)
    }

    private fun checkSwipe() = binding.swipeRefresh.run {
        if (isRefreshing) isRefreshing = !viewModel.isRefreshDone
    }

    private fun setupLiveData() {
        viewModel.apply {
            binding.apply {
                mediatorLiveData.observe(viewLifecycleOwner, ObserverWithSyncChecking{})
                accountsLiveData.observe(viewLifecycleOwner, ObserverWithSyncChecking { accounts ->
                    noDataMessage.visibleOrGone(hasAvailableAccounts)
                    updateAccountsList(accounts)
                    accountAdapter.setFiat(fiatSymbol)
                    setTatsButtonListener()
                })

                dappSessions.observe(
                    viewLifecycleOwner,
                    ObserverWithSyncChecking { sessionsPerAccount ->
                        accountAdapter.updateSessionCount(sessionsPerAccount)
                    })

                balanceStateLiveData.observe(viewLifecycleOwner, ObserverWithSyncChecking { state ->
                    when (state) {
                        is CoinBalanceState -> updateCoinBalanceState(state)
                        is TokenBalanceState -> updateTokenBalanceState(state)
                        is UpdateAllState -> updateAccountsList()
                    }
                })
            }

            errorLiveData.observe(viewLifecycleOwner, EventObserverWithSyncChecking { errorState ->
                when (errorState) {
                    BaseError -> {
                        showErrorFlashbar(R.string.error_header, R.string.unexpected_error)
                    }
                    BalanceIsNotEmptyAndHasMoreOwnersError ->
                        showErrorFlashbar(
                            R.string.cannot_remove_safe_account_title,
                            R.string.cannot_remove_safe_account_message
                        )
                    BalanceIsNotEmptyError ->
                        showErrorFlashbar(
                            R.string.cannot_remove_account_title,
                            R.string.cannot_remove_account_message
                        )
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

    private fun setTatsButtonListener() = binding.apply {
        addCryptoButton.setOnClickListener { view ->
            viewModel.apply {
                if (areMainNetsEnabled) startRampWrappedActivity(requireContext())
            }
        }
    }

    override fun addAccount() = startNewAccountWrappedActivity(requireContext(), getString(R.string.add_account))

    private fun showErrorFlashbar(titleRes: Int, messageRes: Int) =
        MinervaFlashbar.show(requireActivity(), getString(titleRes), getString(messageRes))

    private fun createAccountForSelectedNetwork(chainId: Int) {
        viewModel.createNewAccount(chainId)
    }

    private fun logToFirebaseIfNotSynced() {
        if (!viewModel.isSynced) {
            val firebaseID: String = FirebaseInstanceId.getInstance().id
            logger.logToFirebase("Wallet synchronization error - firebaseId: $firebaseID")
        }
    }

    private inner class EventObserverWithSyncChecking<T>(private val onEventUnhandledContent: (T) -> Unit) :
        Observer<Event<T>> {
        override fun onChanged(event: Event<T>?) {
            event?.getContentIfNotHandled()?.let {
                logToFirebaseIfNotSynced()
                onEventUnhandledContent(it)
            }
        }
    }

    private inner class ObserverWithSyncChecking<T>(private val onValueChanged: (T) -> Unit) :
        Observer<T> {
        override fun onChanged(value: T) {
            logToFirebaseIfNotSynced()
            onValueChanged(value)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AccountsFragment()
        const val ADDRESS = "address"
        const val ITEM = 1 //token info(item) case
        const val ADD_ITEM = -1 //add new account (button) case
        private const val RECEIVE_TRANSACTION_INDEX = 1
    }
}
