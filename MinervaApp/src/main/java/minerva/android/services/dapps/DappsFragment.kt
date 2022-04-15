package minerva.android.services.dapps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.RecyclerViewLayoutBinding
import minerva.android.services.dapps.adapter.DappsAdapter
import minerva.android.services.dapps.adapter.NetworksFilterAdapter
import minerva.android.services.dapps.adapter.SeparatorAdapter
import minerva.android.services.dapps.adapter.TitleAdapter
import minerva.android.services.dapps.dialog.OpenDappDialog
import minerva.android.services.dapps.model.DappsWithCategories
import minerva.android.utils.MyHelper.l
import minerva.android.utils.VerticalMarginItemDecoration
import minerva.android.walletmanager.model.network.Network
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DappsFragment : Fragment(R.layout.recycler_view_layout), DappsAdapter.Listener,
    OpenDappDialog.Listener, NetworksFilterAdapter.Listener {

    private val viewModel: DappsViewModel by sharedViewModel()
    private lateinit var binding: RecyclerViewLayoutBinding
    private lateinit var dialogOpen: OpenDappDialog
    private val dappAdapter = DappsAdapter(this)
    private val sponsoredDappAdapter = DappsAdapter(this)
    private val sponsoredTitleAdapter = TitleAdapter(R.string.sponsored_label)
    private val sponsoredSeparatorAdapter = SeparatorAdapter()
    private val favoriteDappAdapter = DappsAdapter(this)
    private val favoriteTitleAdapter = TitleAdapter(R.string.favorite_label)
    private val favoriteSeparatorAdapter = SeparatorAdapter()
    //networks from networks.json file; uses for filtering dapps by specified network (id)
    private val networksFilterAdapter = NetworksFilterAdapter(this)

    private val concatAdapter = ConcatAdapter(
        favoriteTitleAdapter,
        favoriteDappAdapter,
        favoriteSeparatorAdapter,
        sponsoredTitleAdapter,
        sponsoredDappAdapter,
        sponsoredSeparatorAdapter,
        dappAdapter
    )

    override fun onResume() {
        super.onResume()
        viewModel.getDapps()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = RecyclerViewLayoutBinding.bind(view)
        setupRecycleView()
        setObservers()
    }

    private fun setObservers() {
        with(viewModel) {
            dappsLiveData.observe(
                viewLifecycleOwner,
                Observer { updateDapps(it) }
            )
        }
    }

    private fun updateDapps(dapps: DappsWithCategories) {
        favoriteDappAdapter.submitList(dapps.favorite)
        favoriteTitleAdapter.setVisibility(dapps.isFavoriteVisible)
        favoriteSeparatorAdapter.setVisibility(dapps.isFavoriteVisible)
        sponsoredTitleAdapter.setVisibility(dapps.isSponsoredVisible)
        sponsoredSeparatorAdapter.setVisibility(dapps.isSponsoredVisible)
        sponsoredDappAdapter.submitList(dapps.sponsored)
        dappAdapter.submitList(dapps.remaining)
    }

    private fun setupRecycleView() {
        binding.apply {
            //networks filter recyclerview
            servicesFilterNetworkRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = networksFilterAdapter
                setHasFixedSize(true)
            }
            networksFilterAdapter.submitList(viewModel.filteredNetworks())
            //dapps  recyclerview
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = concatAdapter
                addItemDecoration(getRecyclerViewItemDecorator())
            }
        }
    }

    private fun getRecyclerViewItemDecorator(): VerticalMarginItemDecoration {
        val margin = requireContext().resources.getDimension(R.dimen.margin_small).toInt()
        val topMargin = requireContext().resources.getDimension(R.dimen.margin_xbig).toInt()
        return VerticalMarginItemDecoration(margin, topMargin, margin)
    }

    companion object {
        @JvmStatic
        fun newInstance() = DappsFragment()
    }

    override fun onDappSelected(onDappSelected: DappsAdapter.Listener.OnDappSelected) {
        dialogOpen = OpenDappDialog(dapp = onDappSelected.dapp).apply { listener = this@DappsFragment }
        dialogOpen.show(childFragmentManager, OpenDappDialog.TAG)
    }

    override fun onFavoriteClick(name: String) {
        viewModel.updateFavoriteDapp(name)
    }

    override fun onConfirm(onConfirmData: OpenDappDialog.Listener.OnConfirmData) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(onConfirmData.url))
        startActivity(browserIntent)
    }

    override fun onCancel() {
        dialogOpen.dismiss()
    }

    override fun onNetworkSelected(network: Network) {
        viewModel.filterByNetworkId(network.chainId)
    }
}