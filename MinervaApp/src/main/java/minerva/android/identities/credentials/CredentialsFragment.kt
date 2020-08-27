package minerva.android.identities.credentials

import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.showRemoveDialog
import minerva.android.minervaPrimitive.MinervaPrimitiveListFragment
import minerva.android.walletmanager.model.Credential
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

    override fun onRemoveCredential(credential: Credential) {
        showRemoveDialog(getString(R.string.remove_credential_dialog_title), R.string.remove_credential_dialog_message)
        { viewModel.removeCredential(credential) }
    }

    companion object {
        @JvmStatic
        fun newInstance() = CredentialsFragment()
    }
}