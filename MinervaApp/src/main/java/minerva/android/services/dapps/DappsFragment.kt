package minerva.android.services.dapps

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.recycler_view_layout.view.*
import minerva.android.R
import minerva.android.databinding.RecyclerViewLayoutBinding
import minerva.android.services.dapps.adapter.DappsAdapter
import minerva.android.services.dapps.adapter.SeparatorAdapter
import minerva.android.services.dapps.adapter.TitleAdapter
import minerva.android.services.dapps.dialog.OpenDappDialog
import minerva.android.services.dapps.model.DappsWithCategories
import minerva.android.utils.MyHelper.l
import minerva.android.utils.VerticalMarginItemDecoration
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DappsFragment : Fragment(R.layout.recycler_view_layout), DappsAdapter.Listener,
    OpenDappDialog.Listener {

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

    override fun onPause() {
        super.onPause()
        binding?.servicesFilterChipGroup.removeAllViews() //remove all child from ChipGrop
        networkFilterCreated = false //for recreate filter when fragment get back to visible
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

    private fun updateDapps(dapps: DappsWithCategories) { l("updateDapps")
        favoriteDappAdapter.submitList(dapps.favorite)
        favoriteTitleAdapter.setVisibility(dapps.isFavoriteVisible)
        favoriteSeparatorAdapter.setVisibility(dapps.isFavoriteVisible)
        sponsoredTitleAdapter.setVisibility(dapps.isSponsoredVisible)
        sponsoredSeparatorAdapter.setVisibility(dapps.isSponsoredVisible)
        sponsoredDappAdapter.submitList(dapps.sponsored)
        dappAdapter.submitList(dapps.remaining)
        initializeNetworkFilter()
    }

    /**
     * Update Network Filter - method which update Network filter by current choosen state
     * @param currentChipId: Int - id of element which must be set as enable
     */
    private fun updateNetworkFilter(currentChipId: Int) {
        binding.servicesFilterChipGroup.apply {
            //multiple press default elements - prevent future actions
            if (prevChipId == allDappItemChainId && currentChipId == allDappItemChainId) return
            //re-clicked the selected - will enable default element
            if (prevChipId == currentChipId) {
                children.forEach { child -> //iterate each Chip in ChipGroup
                    if (child.id == currentChipId) //set each item like disabled (except default)
                        updateNetworkItemStyle((child as Chip), false)
                    else if (child.id == allDappItemChainId) //set default item like enabled
                        updateNetworkItemStyle((child as Chip), true)
                }
                prevChipId = allDappItemChainId //set default as prev selected item
            } else {
                children.forEach { child ->
                    if (child.id == currentChipId) //set style for selected item
                        updateNetworkItemStyle((child as Chip), true)
                    else updateNetworkItemStyle((child as Chip), false) //set style for other items
                }
                //set selected item as prev selected item(for change it style when item changed)
                prevChipId = currentChipId
            }
            //resort dapps cache by specified network chainId
            viewModel.filterByNetworkId(prevChipId)
            l("v8")
        }
    }

    /**
     * Update Network Item Style - method which change style of specified element
     * @param chip: Chip - element with style need to be changed
     * @param status: Boolean - if "true" set item to "enabled" style; if "false" set item to "disabled"
     */
    private fun updateNetworkItemStyle(chip: Chip, status: Boolean) {
        if (status == true) {
            chip.setChipBackgroundColorResource(R.color.servicesFilterNetworkMainColor)
            chip.setTextColor(Color.WHITE)
        } else {
            chip.setChipBackgroundColorResource(R.color.white)
            chip.setTextColor(Color.parseColor(networkFilterMainColor))
        }
    }

    /**
     * Initialize Network Filter - method which set default state of network filter
     */
    private fun initializeNetworkFilter() {
        if (networkFilterCreated) return
        binding?.servicesFilterChipGroup.apply {
            viewModel.getFilteredNetworks().forEach { network ->
                //set chip item according to Network data
                val chip = layoutInflater.inflate(
                    R.layout.services_network_filter_chip_item, services_filter_chip_group, false) as Chip
                chip.id = network.chainId //network.chainId as chip id
                chip.setText(network.name)
                chip.setCheckedIconVisible(false)
                chip.setOnClickListener { selectedNetwork ->
                    updateNetworkFilter(selectedNetwork.id)
                }
                addView(chip) //adding chip to chip group
            }
            //set default element (allDappItemChainId) at first enable item
            val defNetwork: Chip = (children.find { it.id == allDappItemChainId } as Chip)
            updateNetworkItemStyle(defNetwork, true)
            //resort dapps data (cache) by specified network chainId
            viewModel.filterByNetworkId(defNetwork.id)
        }
        networkFilterCreated = true //prevent multiple creation of filter
    }

    private fun setupRecycleView() {
        binding.apply {
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
        val allDappItemName = "All"
        val allDappItemChainId = -2 //id for default filler item ("-1" kotlin will convert to "1" thats why use "-2")
        private var prevChipId = allDappItemChainId //store prev selected f. item(for change it style when new item choose)
        private var networkFilterCreated = false //prevent multiple creation of filter
        private val networkFilterMainColor = "#5858ED" //main color of network filter
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
}