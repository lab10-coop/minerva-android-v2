package minerva.android.services.connections


import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.minervaPrimitive.MinervaPrimitiveListFragment
import minerva.android.services.ServicesViewModel
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.walletconnect.DappSessionV1
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.walletconnect.DappSessionV2
import org.koin.androidx.viewmodel.ext.android.viewModel


class ConnectionsFragment : MinervaPrimitiveListFragment() {

    private val viewModel: ServicesViewModel by viewModel()

    override fun prepareObservers() {
        with(viewModel) {
            servicesLiveData.observe(viewLifecycleOwner, Observer { updateList(it) })
            dappSessionsLiveData.observe(viewLifecycleOwner, Observer { updateList(it) })
            serviceRemovedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.invalidateOptionsMenu() })
            errorLiveData.observe(viewLifecycleOwner, EventObserver { handleAutomaticBackupError(it) })
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

    override fun onRemoveDappSession(dapp: DappSessionV1) {
        viewModel.removeSession(dapp)
    }

    override fun onEndDappSession(dapp: DappSessionV2) {
        viewModel.removeSession(dapp)
    }

    override fun onRemovePairing(dapp: MinervaPrimitive) {
        viewModel.removePairing(dapp)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ConnectionsFragment()
    }
}