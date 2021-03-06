package minerva.android.walletActions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletActions.adapter.WalletActionsAdapter
import minerva.android.walletmanager.model.WalletActionClustered
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class WalletActionsFragment : Fragment() {

    private val viewModel: WalletActionsViewModel by viewModel()
    private val walletActionsAdapter = WalletActionsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView(view)
        prepareObservers()
    }

    private fun prepareObservers() {
        viewModel.apply {
            walletActionsLiveData.observe(this@WalletActionsFragment, EventObserver { handleWalletActionsLiveData(it) })
            errorLiveData.observe(this@WalletActionsFragment, EventObserver {
                MinervaFlashbar.show(requireActivity(), getString(R.string.error_header), getString(R.string.activities_error))
            })
            loadingLiveData.observe(this@WalletActionsFragment, EventObserver { handleLoadingLiveData(it) })
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchWalletActions()
    }

    private fun handleWalletActionsLiveData(it: List<WalletActionClustered>) {
        if (it.isEmpty()) {
            progressBar.gone()
            noDataMessage.visible()
        } else {
            progressBar.gone()
            walletActionsAdapter.updateList(it)
        }
    }

    private fun handleLoadingLiveData(isShowing: Boolean) {
        if (isShowing) {
            progressBar.gone()
        } else {
            progressBar.visible()
            noDataMessage.gone()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun setupRecycleView(view: View) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = walletActionsAdapter
        }
    }
}
