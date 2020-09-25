package minerva.android.identities.credentials

import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.showRemoveDialog
import minerva.android.minervaPrimitive.MinervaPrimitiveListFragment
import minerva.android.walletmanager.model.Credential
import minerva.android.widget.clubCard.OamtcClubCard
import org.koin.androidx.viewmodel.ext.android.viewModel

class CredentialsFragment : MinervaPrimitiveListFragment() {

    private val viewModel: CredentialsViewModel by viewModel()

    override fun prepareObservers() {
        viewModel.apply {
            walletConfigLiveData.observe(viewLifecycleOwner, Observer { config ->
                noDataMessage.visibleOrGone(config.credentials.isEmpty())
                primitivesAdapter.updateList(config.credentials)
            })
            removeCredentialMutableLiveData.observe(viewLifecycleOwner, Observer {
                activity?.invalidateOptionsMenu()
            })
        }
    }

    override fun onRemoveCredential(credential: Credential) =
        showRemoveDialog(getString(R.string.remove_credential_dialog_title), R.string.remove_credential_dialog_message)
        { viewModel.removeCredential(credential) }

    override fun onCredentialContainerClick(credential: Credential) = OamtcClubCard(requireContext(), credential).show()

    override fun getLoggedIdentityName(loggedInIdentityDid: String): String = viewModel.getLoggedIdentityName(loggedInIdentityDid)

    companion object {
        @JvmStatic
        fun newInstance() = CredentialsFragment()
    }
}