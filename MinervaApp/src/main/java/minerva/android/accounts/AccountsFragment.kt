package minerva.android.accounts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.refreshable_recycler_view_layout.*
import minerva.android.R
import minerva.android.accounts.adapter.AccountAdapter
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.kotlinUtils.function.orElse
import minerva.android.main.listener.FragmentInteractorListener
import minerva.android.walletmanager.model.Account
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class AccountsFragment : Fragment(), AccountsFragmentToAdapterListener {

    private val viewModel: AccountsViewModel by viewModel()
    private val accountAdapter by lazy { AccountAdapter(this) }
    private lateinit var listener: FragmentInteractorListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.refreshable_recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView(view)
        setupLiveData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.apply {
            onResume()
            refreshBalances()
            refreshAssetBalance()
        }

        if (viewModel.arePendingAccountsEmpty()) {
            accountAdapter.stopPendingTransactions()
        }
    }

    fun refreshBalances() {
        viewModel.refreshBalances()
    }

    fun stopPendingTransactions() {
        accountAdapter.stopPendingTransactions()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as FragmentInteractorListener
    }

    override fun onSendTransaction(account: Account) = listener.showSendTransactionScreen(account)

    override fun onSendAssetTransaction(accountIndex: Int, assetIndex: Int) {
        listener.showSendAssetTransactionScreen(accountIndex, assetIndex)
    }

    override fun onCreateSafeAccount(account: Account) = viewModel.createSafeAccount(account)

    override fun onAccountRemove(account: Account) = showRemoveDialog(account)

    fun setProgressAccount(index: Int, pending: Boolean) {
        accountAdapter.setPending(index, pending)
    }

    private fun setupRecycleView(view: View) {
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

    private fun setupLiveData() {
        viewModel.apply {
            walletConfigLiveData.observe(viewLifecycleOwner, Observer {
                noDataMessage.visibleOrGone(it.hasActiveAccount)
                accountAdapter.updateList(it.accounts)
            })
            balanceLiveData.observe(viewLifecycleOwner, Observer {
                accountAdapter.updateBalances(it)
                swipeRefresh.isRefreshing = false
            })
            accountAssetBalanceLiveData.observe(viewLifecycleOwner, Observer { accountAdapter.updateAssetBalances(it) })
            errorLiveData.observe(viewLifecycleOwner, Observer {
                showErrorFlashbar(getString(R.string.error_header), it.peekContent().message)
            })
            noFundsLiveData.observe(viewLifecycleOwner, Observer {
                MinervaFlashbar.show(requireActivity(), getString(R.string.no_funds), getString(R.string.no_funds_message))
            })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver {
                listener.shouldShowLoadingScreen(it)
            })
            balanceIsNotEmptyAndHasMoreOwnersErrorLiveData.observe(viewLifecycleOwner, EventObserver {
                showErrorFlashbar(getString(R.string.cannot_remove_safe_account_title), getString(R.string.cannot_remove_safe_account_message))
            })
            isNotSafeAccountMasterOwnerErrorLiveData.observe(viewLifecycleOwner, EventObserver {
                showErrorFlashbar(getString(R.string.error_header), getString(R.string.safe_account_removal_error))
            })
            accountRemovedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.invalidateOptionsMenu() })
        }
    }

    private fun showErrorFlashbar(title: String, message: String? = String.Empty) =
        message?.let {
            MinervaFlashbar.show(requireActivity(), title, it)
        }.orElse {
            MinervaFlashbar.show(requireActivity(), title, getString(R.string.unexpected_error))
        }

    private fun showRemoveDialog(account: Account) {
        context?.let { context ->
            MaterialAlertDialogBuilder(context, R.style.AlertDialogMaterialTheme)
                .setBackground(context.getDrawable(R.drawable.rounded_white_background))
                .setTitle(account.name)
                .setMessage(R.string.remove_account_dialog_message)
                .setPositiveButton(R.string.remove) { dialog, _ ->
                    viewModel.removeAccount(account)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
