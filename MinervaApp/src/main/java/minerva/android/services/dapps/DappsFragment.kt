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
import minerva.android.services.dapps.adapter.SeparatorAdapter
import minerva.android.services.dapps.adapter.TitleAdapter
import minerva.android.services.dapps.dialog.OpenDappDialog
import minerva.android.services.dapps.model.DappsWithCategories
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
    private val separatorAdapter = SeparatorAdapter()

    private val concatAdapter = ConcatAdapter(
        sponsoredTitleAdapter, sponsoredDappAdapter, separatorAdapter, dappAdapter
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
        sponsoredTitleAdapter.setVisibility(dapps.isSponsoredVisible)
        separatorAdapter.setVisibility(dapps.isSponsoredVisible)
        sponsoredDappAdapter.submitList(dapps.sponsored)
        dappAdapter.submitList(dapps.remaining)
    }

    private fun setupRecycleView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = concatAdapter
            addItemDecoration(getRecyclerViewItemDecorator())
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

    override fun onConfirm(onConfirmData: OpenDappDialog.Listener.OnConfirmData) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(onConfirmData.url))
        startActivity(browserIntent)
    }

    override fun onCancel() {
        dialogOpen.dismiss()
    }
}