package minerva.android.services

import android.widget.Toast
import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.minervaPrimitive.MinervaPrimitiveListFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.MinervaPrimitive
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
            servicesLiveData.observe(viewLifecycleOwner, Observer { updateList(it) })
            dappSessionsLiveData.observe(viewLifecycleOwner, Observer { updateList(it) })
            serviceRemovedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.invalidateOptionsMenu() })
            errorLiveData.observe(viewLifecycleOwner, EventObserver { handleAutomaticBackupError(it) })
            dappRemovedLiveData.observe(viewLifecycleOwner, EventObserver {
                Toast.makeText(context, getString(R.string.dapp_deleted), Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun updateList(it: List<MinervaPrimitive>) {
        binding.noDataMessage.visibleOrGone(it.isEmpty())
        primitivesAdapter.updateList(it)
    }

    override fun onRemoveService(service: Service) = with(service) {
        AlertDialogHandler.showRemoveDialog(requireContext(), name, getString(R.string.remove_service_dialog_message))
        { viewModel.removeService(issuer, name) }
    }

    override fun onRemoveDappSession(dapp: DappSession) {
        viewModel.removeSession(dapp)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ServicesFragment()
    }
}
