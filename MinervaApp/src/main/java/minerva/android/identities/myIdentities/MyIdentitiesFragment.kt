package minerva.android.identities.myIdentities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.identities.MinervaPrimitivesViewModel
import minerva.android.identities.adapter.IdentityAdapter
import minerva.android.identities.adapter.IdentityFragmentListener
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.utils.AlertDialogHandler
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.walletmanager.model.Service
import minerva.android.wrapped.startEditIdentityWrappedActivity
import minerva.android.wrapped.startIdentityAddressWrappedActivity
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MyIdentitiesFragment : Fragment(), IdentityFragmentListener {

    private val viewModel: MinervaPrimitivesViewModel by sharedViewModel()
    private val identityAdapter = IdentityAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        setObservers()
    }

    private fun setObservers() {
        with(viewModel) {
            walletConfigLiveData.observe(
                viewLifecycleOwner,
                Observer { identityAdapter.updateList(it.identities.toMutableList(), it.credentials) })
            identityRemovedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.invalidateOptionsMenu() })
        }
    }

    private fun setupRecycleView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = identityAdapter
        }
    }

    override fun showIdentity(identity: Identity, position: Int) {
        startIdentityAddressWrappedActivity(requireContext(), identity.name, position)
    }

    override fun onIdentityRemoved(identity: Identity) {
        AlertDialogHandler.showRemoveDialog(
            requireContext(),
            identity.name,
            getString(R.string.remove_identity_dialog_message)
        ) { viewModel.removeIdentity(identity) }
    }

    override fun onIdentityEdit(position: Int, name: String) {
        startEditIdentityWrappedActivity(requireContext(), position, name)
    }

    override fun onBindedItemDeleted(minervaPrimitive: MinervaPrimitive) {
        when (minervaPrimitive) {
            is Service -> TODO("Handle deleting binded service")
            is Credential -> AlertDialogHandler.showRemoveDialog(
                requireContext(),
                getString(R.string.remove_credential_dialog_title),
                getString(R.string.remove_credential_dialog_message)
            )
            { viewModel.removeCredential(minervaPrimitive) }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MyIdentitiesFragment()
    }
}