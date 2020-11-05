package minerva.android.services

import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.minervaPrimitive.MinervaPrimitiveListFragment
import minerva.android.utils.DialogHandler
import minerva.android.walletmanager.model.Service
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
            errorLiveData.observe(viewLifecycleOwner, EventObserver { handleAutomaticBackupError(it) })
        }
    }

    override fun onRemoveService(service: Service) {
        DialogHandler.showRemoveDialog(requireContext(), service.name, getString(R.string.remove_service_dialog_message)) {
            viewModel.removeService(
                service.issuer,
                service.name
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ServicesFragment()
    }
}
