package minerva.android.walletActions

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.RecyclerViewLayoutBinding
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.main.base.BaseFragment
import minerva.android.walletActions.adapter.WalletActionsAdapter
import minerva.android.walletmanager.model.wallet.WalletActionClustered
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class WalletActionsFragment : BaseFragment(R.layout.recycler_view_layout) {

    private val viewModel: WalletActionsViewModel by viewModel()
    private val walletActionsAdapter by lazy { WalletActionsAdapter() }
    private lateinit var binding: RecyclerViewLayoutBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = RecyclerViewLayoutBinding.bind(view)
        setupRecycleView(view)
        prepareObservers()
    }

    private fun prepareObservers() {
        viewModel.apply {
            walletActionsLiveData.observe(viewLifecycleOwner, EventObserver { handleWalletActionsLiveData(it) })
            errorLiveData.observe(viewLifecycleOwner, EventObserver {
                MinervaFlashbar.show(requireActivity(), getString(R.string.error_header), getString(R.string.activities_error))
            })
            loadingLiveData.observe(viewLifecycleOwner, EventObserver { handleLoadingLiveData(it) })
        }
    }

    override fun onResume() {
        super.onResume()
        interactor.changeActionBarColor(R.color.lightGray)
        viewModel.fetchWalletActions()
    }

    private fun handleWalletActionsLiveData(it: List<WalletActionClustered>) {
        binding.apply {
            if (it.isEmpty()) {
                progressBar.gone()
                noDataMessage.visible()
            } else {
                progressBar.gone()
                walletActionsAdapter.updateList(it)
            }
        }
    }

    private fun handleLoadingLiveData(isShowing: Boolean) {
        binding.apply {
            if (isShowing) {
                progressBar.gone()
            } else {
                progressBar.visible()
                noDataMessage.gone()
            }
        }
    }

    private fun setupRecycleView(view: View) {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = walletActionsAdapter
        }
    }
}
