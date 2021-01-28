package minerva.android.services

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.minervaPrimitive.MinervaPrimitiveListFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.Service
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServicesFragment : MinervaPrimitiveListFragment() {

    private val viewModel: ServicesViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        viewModel.setDappSessionsFlowable()
    }

    override fun onResume() {
        super.onResume()
        interactor.changeActionBarColor(R.color.lightGray)
    }

    override fun prepareObservers() {
        viewModel.apply {
            walletConfigLiveData.observe(viewLifecycleOwner, Observer {
                binding.noDataMessage.visibleOrGone(it.isEmpty())
                primitivesAdapter.updateList(it)
            })
            dappSessionsLiveData.observe(viewLifecycleOwner, Observer {
                primitivesAdapter.updateList(it)
            })
            serviceRemovedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.invalidateOptionsMenu() })
            errorLiveData.observe(viewLifecycleOwner, EventObserver { handleAutomaticBackupError(it) })
        }
    }

    override fun onRemoveService(service: Service) = with(service) {
        AlertDialogHandler.showRemoveDialog(requireContext(), name, getString(R.string.remove_service_dialog_message))
        { viewModel.removeService(issuer, name) }
    }

    override fun onRemoveDappSession(dapp: DappSession) {
        Toast.makeText(context, dapp.name, Toast.LENGTH_SHORT).show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ServicesFragment()
    }
}
