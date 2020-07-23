package minerva.android.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.services.adapter.ServicesAdapter
import minerva.android.services.listener.ServicesMenuListener
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServicesFragment : Fragment(), ServicesMenuListener {

    private val servicesAdapter = ServicesAdapter(this)
    private val viewModel: ServicesViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        prepareWalletConfigObserver()
    }

    private fun prepareWalletConfigObserver() {
        viewModel.apply {
            walletConfigLiveData.observe(viewLifecycleOwner, Observer {
                noDataMessage.visibleOrGone(it.services.isEmpty())
                servicesAdapter.updateList(it.services)
            })
            serviceRemovedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.invalidateOptionsMenu() })
            errorLiveData.observe(viewLifecycleOwner, Observer {
                MinervaFlashbar.show(requireActivity(), getString(R.string.error_header), getString(R.string.unexpected_error))
            })
        }
    }

    private fun setupRecycleView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = servicesAdapter
        }
    }

    override fun onRemoved(type: String, name: String) {
        showRemoveDialog(type, name)
    }

    private fun showRemoveDialog(type: String, name: String) {
        context?.let { context ->
            MaterialAlertDialogBuilder(context, R.style.AlertDialogMaterialTheme)
                .setBackground(context.getDrawable(R.drawable.rounded_white_background))
                .setTitle(name)
                .setMessage(R.string.remove_service_dialog_message)
                .setPositiveButton(R.string.remove) { dialog, _ ->
                    viewModel.removeService(type, name)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
