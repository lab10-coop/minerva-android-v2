package minerva.android.accounts.transaction.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.accounts.adapter.AccountAdapter
import minerva.android.accounts.enum.ErrorCode
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.databinding.RefreshableRecyclerViewLayoutBinding
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.base.BaseFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.Account
import minerva.android.widget.MinervaFlashbar
import minerva.android.widget.dialog.FundsAtRiskDialog
import minerva.android.wrapped.startManageAssetsWrappedActivity
import minerva.android.wrapped.startSafeAccountWrappedActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class AccountsFragment : BaseFragment(R.layout.refreshable_recycler_view_layout), AccountsFragmentToAdapterListener {

    private val viewModel: AccountsViewModel by viewModel()
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
            refreshBalances()
            refreshAssetBalance()
            refreshFreeATSButton()
            if (arePendingAccountsEmpty()) accountAdapter.stopPendingTransactions()
        }
    }

    fun refreshBalances() = viewModel.refreshBalances()

    fun stopPendingTransactions() = accountAdapter.stopPendingTransactions()

    override fun onSendTransaction(index: Int) = interactor.showTransactionScreen(index)

    override fun onSendAssetTransaction(accountIndex: Int, assetIndex: Int) =
        interactor.showTransactionScreen(accountIndex, assetIndex)

    override fun onCreateSafeAccount(account: Account) = viewModel.createSafeAccount(account)

    override fun onAccountRemove(account: Account) =
        AlertDialogHandler.showRemoveDialog(
            requireContext(),
            account.name,
            getString(R.string.remove_account_dialog_message)
        ) { viewModel.removeAccount(account) }

    override fun onShowAddress(accountIndex: Int) =
        interactor.showTransactionScreen(accountIndex, screenIndex = RECEIVE_TRANSACTION_INDEX)

    override fun onShowSafeAccountSettings(account: Account, position: Int) =
        startSafeAccountWrappedActivity(requireContext(), account.name, position, account.network.short, account.isSafeAccount)

    override fun onWalletConnect() = interactor.showWalletConnectScanner()

    override fun onManageAssets(index: Int) = startManageAssetsWrappedActivity(requireContext(), index)

    override fun isAssetVisible(networkAddress: String, assetAddress: String): Boolean? =
        viewModel.isAssetVisible(networkAddress, assetAddress)

    override fun saveAssetVisibility(networkAddress: String, assetAddress: String, visibility: Boolean) {
        viewModel.saveAssetVisible(networkAddress, assetAddress, visibility)
    }

    fun setPendingAccount(index: Int, pending: Boolean) {
        accountAdapter.setPending(index, pending, viewModel.areMainNetsEnabled)
    }

    private fun initFragment() {
        binding.apply {
            viewModel.apply {
                networksHeader.text = getHeader(areMainNetsEnabled)
                addTatsButton.visibleOrGone(!areMainNetsEnabled)
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
                        refreshBalances()
                        refreshAssetBalance()
                    }
                }
            }

            recyclerView.apply {
                layoutManager = LinearLayoutManager(view.context)
                adapter = accountAdapter
            }
        }
    }

    private fun setupLiveData() {
        viewModel.apply {
            shouldShowWarringLiveData.observe(viewLifecycleOwner, EventObserver {
                if (it) {
                    FundsAtRiskDialog(requireContext()).show()
                }
            })
            binding.apply {
                walletConfigLiveData.observe(viewLifecycleOwner, Observer { walletConfig ->
                    noDataMessage.visibleOrGone(walletConfig.hasActiveAccount)
                    accountAdapter.updateList(walletConfig.accounts, areMainNetsEnabled)
                    setTatsButtonListener(accountAdapter.activeAccountsList)
                })
                balanceLiveData.observe(viewLifecycleOwner, Observer {
                    accountAdapter.updateBalances(it)
                    swipeRefresh.isRefreshing = false
                })
            }
            accountAssetBalanceLiveData.observe(viewLifecycleOwner, Observer { accountAdapter.updateAssetBalances(it) })
            errorLiveData.observe(viewLifecycleOwner, EventObserver {
                showErrorFlashbar(
                    getString(R.string.error_header),
                    getString(R.string.unexpected_error)
                )
            })

            noFundsLiveData.observe(viewLifecycleOwner, Observer {
                MinervaFlashbar.show(
                    requireActivity(),
                    getString(R.string.no_funds),
                    getString(R.string.no_funds_message)
                )
            })

            loadingLiveData.observe(viewLifecycleOwner, EventObserver {
                interactor.shouldShowLoadingScreen(it)
            })

            balanceIsNotEmptyAndHasMoreOwnersErrorLiveData.observe(viewLifecycleOwner, EventObserver {
                showErrorFlashbar(
                    getString(R.string.cannot_remove_safe_account_title),
                    getString(R.string.cannot_remove_safe_account_message)
                )
            })

            balanceIsNotEmptyErrorLiveData.observe(viewLifecycleOwner, EventObserver {
                showErrorFlashbar(
                    getString(R.string.cannot_remove_account_title),
                    getString(R.string.cannot_remove_account_message)
                )
            })

            isNotSafeAccountMasterOwnerErrorLiveData.observe(viewLifecycleOwner, EventObserver {
                showErrorFlashbar(getString(R.string.error_header), getString(R.string.safe_account_removal_error))
            })

            accountRemovedLiveData.observe(viewLifecycleOwner, EventObserver {
                activity?.invalidateOptionsMenu()
                refreshFreeATSButton()
            })

            automaticBackupErrorLiveData.observe(viewLifecycleOwner, EventObserver { handleAutomaticBackupError(it) })
            refreshBalancesErrorLiveData.observe(viewLifecycleOwner, EventObserver { handleRefreshBalancesError(it) })
        }
    }

    private fun handleRefreshBalancesError(it: ErrorCode) {
        binding.swipeRefresh.isRefreshing = false
        when (it) {
            ErrorCode.BALANCE_ERROR -> showErrorFlashbar(
                getString(R.string.error_header),
                getString(R.string.refresh_balances_error)
            )
            ErrorCode.ASSET_BALANCE_ERROR -> showErrorFlashbar(
                getString(R.string.error_header),
                getString(R.string.refresh_asset_balances_error)
            )
        }
    }

    private fun refreshFreeATSButton() {
        viewModel.isAddingFreeATSAvailable(accountAdapter.activeAccountsList).let { isAvailable ->
            binding.addTatsButton.apply {
                val color = if (isAvailable) R.color.artis else R.color.inactiveButtonColor
                setBackgroundColor(ContextCompat.getColor(context, color))
            }
        }
    }

    private fun setTatsButtonListener(accounts: List<Account>) =
        binding.addTatsButton.setOnClickListener {
            viewModel.apply {
                Toast.makeText(it.context, R.string.free_ats_warning, Toast.LENGTH_SHORT).show()
                if (isAddingFreeATSAvailable(accountAdapter.activeAccountsList)) {
                    it.setBackgroundColor(ContextCompat.getColor(it.context, R.color.inactiveButtonColor))
                    addAtsToken(accounts, getString(R.string.free_ats_warning))
                }
            }
        }

    private fun showErrorFlashbar(title: String, message: String? = String.Empty) =
        message?.let {
            MinervaFlashbar.show(requireActivity(), title, it)
        }.orElse {
            MinervaFlashbar.show(requireActivity(), title, getString(R.string.unexpected_error))
        }

    companion object {
        private const val RECEIVE_TRANSACTION_INDEX = 1
    }
}
