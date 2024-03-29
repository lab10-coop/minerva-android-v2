package minerva.android.identities.myIdentities

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.RecyclerViewLayoutBinding
import minerva.android.identities.MinervaPrimitivesViewModel
import minerva.android.identities.adapter.IdentityAdapter
import minerva.android.identities.adapter.IdentityFragmentListener
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.utils.AlertDialogHandler
import minerva.android.utils.VerticalMarginItemDecoration
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.wrapped.startEditIdentityWrappedActivity
import minerva.android.wrapped.startIdentityAddressWrappedActivity
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MyIdentitiesFragment : Fragment(R.layout.recycler_view_layout), IdentityFragmentListener {

    private val viewModel: MinervaPrimitivesViewModel by sharedViewModel()
    private lateinit var binding: RecyclerViewLayoutBinding
    private val identityAdapter = IdentityAdapter(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = RecyclerViewLayoutBinding.bind(view)
        setupRecycleView()
        setObservers()
    }

    private fun setObservers() {
        with(viewModel) {
            walletConfigLiveData.observe(
                viewLifecycleOwner,
                Observer {
                    it.peekContent().apply {
                        identityAdapter.updateList(identities.toMutableList(), credentials)
                    }
                })
            identityRemovedLiveData.observe(viewLifecycleOwner, EventObserver { activity?.invalidateOptionsMenu() })
        }
    }

    private fun setupRecycleView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = identityAdapter
            addItemDecoration(getRecyclerViewItemDecorator())
        }
    }

    private fun getRecyclerViewItemDecorator(): VerticalMarginItemDecoration {
        val margin = requireContext().resources.getDimension(R.dimen.margin_small).toInt()
        val bottomMargin = requireContext().resources.getDimension(R.dimen.margin_xbig).toInt()
        return VerticalMarginItemDecoration(margin, margin, bottomMargin)
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