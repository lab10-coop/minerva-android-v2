package minerva.android.accounts.nft.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.accounts.nft.viewmodel.NftCollectionViewModel
import minerva.android.databinding.FragmentNftCollectionBinding
import minerva.android.extension.visibleOrGone
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NftCollectionFragment : Fragment(R.layout.fragment_nft_collection) {

    private val viewModel: NftCollectionViewModel by viewModel {
        parametersOf(arguments?.getInt(ACCOUNT_ID), arguments?.getString(COLLECTION_ADDRESS))
    }
    private lateinit var binding: FragmentNftCollectionBinding
    private val nftAdapter by lazy { NftAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNftCollectionBinding.bind(view)
        setupRecyclerView()
        setupObserver()
        initList()
    }

    private fun initList() = viewModel.getNftForCollection()

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = nftAdapter
    }

    private fun setupObserver() = with(viewModel) {
        nftListLiveData.observe(viewLifecycleOwner, Observer { list ->
            nftAdapter.updateList(list)
        })
        loadingLiveData.observe(viewLifecycleOwner, Observer { handleLoadingState(it) })
    }

    private fun handleLoadingState(isVisible: Boolean) = with(binding.progress) {
        if (!isVisible) cancelAnimation()
        visibleOrGone(isVisible)
    }

    companion object {
        private const val COLLECTION_ADDRESS = "collection_address"
        private const val ACCOUNT_ID = "account_id"

        @JvmStatic
        fun newInstance(accountId: Int, collectionAddress: String) = NftCollectionFragment().apply {
            arguments = Bundle().apply {
                putInt(ACCOUNT_ID, accountId)
                putString(COLLECTION_ADDRESS, collectionAddress)
            }
        }
    }
}