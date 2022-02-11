package minerva.android.accounts.nft.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.accounts.nft.model.NftItem
import minerva.android.accounts.nft.viewmodel.NftCollectionViewModel
import minerva.android.databinding.FragmentNftCollectionBinding
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.NO_MARGIN
import minerva.android.utils.VerticalMarginItemDecoration

class NftCollectionFragment : Fragment(R.layout.fragment_nft_collection) {

    private val viewModel: NftCollectionViewModel by activityViewModels()

    private lateinit var binding: FragmentNftCollectionBinding

    private val nftAdapter by lazy {
        NftAdapter().apply {
            listener = object : NftViewHolder.Listener {
                override fun onSendClicked(nftItem: NftItem) {
                    viewModel.selectItem(nftItem)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNftCollectionBinding.bind(view)
        setupRecyclerView()
        setupObserver()
        requireActivity().invalidateOptionsMenu()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = nftAdapter
            addItemDecoration(getRecyclerViewItemDecorator())
        }
    }

    private fun getRecyclerViewItemDecorator(): VerticalMarginItemDecoration {
        val margin = requireContext().resources.getDimension(R.dimen.margin_normal).toInt()
        return VerticalMarginItemDecoration(margin, Int.NO_MARGIN, Int.NO_MARGIN)
    }

    private fun setupObserver() = with(viewModel) {
        nftListLiveData.observe(viewLifecycleOwner, Observer { list ->
            binding.noNftsAvailableLabel.visibleOrGone(list.isEmpty())
            nftAdapter.updateList(list)
        })
        loadingLiveData.observe(viewLifecycleOwner, Observer { handleLoadingState(it) })
    }

    private fun handleLoadingState(isVisible: Boolean) = with(binding.progress) {
        if (!isVisible) cancelAnimation()
        visibleOrGone(isVisible)
    }

    companion object {
        @JvmStatic
        fun newInstance() = NftCollectionFragment()
    }
}