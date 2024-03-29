package minerva.android.identities.credentials

import androidx.lifecycle.Observer
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.identities.MinervaPrimitivesViewModel
import minerva.android.minervaPrimitive.MinervaPrimitiveListFragment
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.widget.clubCard.ClubCard
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class CredentialsFragment : MinervaPrimitiveListFragment() {

    private val viewModel: MinervaPrimitivesViewModel by sharedViewModel()

    override fun prepareObservers() {
        viewModel.apply {
            walletConfigLiveData.observe(viewLifecycleOwner, Observer { config ->
                with(config.peekContent().credentials){
                    binding.noDataMessage.visibleOrGone(isEmpty())
                    primitivesAdapter.updateList(this)
                }
            })
            removeCredentialLiveData.observe(viewLifecycleOwner, Observer {
                activity?.invalidateOptionsMenu()
            })
        }
    }

    override fun onRemoveCredential(credential: Credential) =
        AlertDialogHandler.showRemoveDialog(
            requireContext(),
            getString(R.string.remove_credential_dialog_title),
            getString(R.string.remove_credential_dialog_message)
        )
        { viewModel.removeCredential(credential) }

    override fun onCredentialContainerClick(credential: Credential) = ClubCard(requireContext(), credential).show()

    override fun getLoggedIdentityName(loggedInIdentityDid: String): String = viewModel.getLoggedIdentityName(loggedInIdentityDid)

    companion object {
        @JvmStatic
        fun newInstance() = CredentialsFragment()
    }
}