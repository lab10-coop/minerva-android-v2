package minerva.android.services.dapps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.RecyclerViewLayoutBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import android.net.Uri

import android.content.Intent
import minerva.android.services.dapps.adapter.DappsAdapter
import minerva.android.services.dapps.dialog.OpenDappDialog


class DappsFragment : Fragment(R.layout.recycler_view_layout), DappsAdapter.Listener,
    OpenDappDialog.Listener {

    private val viewModel: DappsViewModel by sharedViewModel()
    private lateinit var binding: RecyclerViewLayoutBinding
    private lateinit var dialogOpen: OpenDappDialog
    private val dappAdapter = DappsAdapter(this)

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
                Observer {
                    dappAdapter.submitList(it)
                }
            )
        }
    }

    private fun setupRecycleView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = dappAdapter
        }
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