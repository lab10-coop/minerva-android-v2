package minerva.android.services

import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.showRemoveDialog
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.minervaPrimitive.MinervaPrimitiveListFragment
import minerva.android.walletmanager.model.Service
import minerva.android.widget.MinervaFlashbar
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServicesFragment : MinervaPrimitiveListFragment() {

    private val viewModel: ServicesViewModel by viewModel()

    override fun onResume() {
        super.onResume()
        interactor.changeActionBarColor(R.color.lightGray)
    }

    override fun prepareObservers() {
        viewModel.apply {
            walletConfigLiveData.observe(viewLifecycleOwner, Observer {
                noDataMessage.visibleOrGone(it.services.isEmpty())
                primitivesAdapter.updateList(it.services)
            })
            serviceRemovedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.invalidateOptionsMenu() })
            errorLiveData.observe(viewLifecycleOwner, Observer {
                MinervaFlashbar.show(requireActivity(), getString(R.string.error_header), getString(R.string.unexpected_error))
            })
        }
    }

    override fun onRemoveService(service: Service) {
        showRemoveDialog(service.name, R.string.remove_service_dialog_message) { viewModel.removeService(service.issuer, service.name) }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ServicesFragment()
    }
}
