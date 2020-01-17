package minerva.android.identities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.identities.adapter.IdentityAdapter
import minerva.android.kotlinUtils.event.EventObserver
import minerva.android.walletmanager.model.Identity
import org.koin.androidx.viewmodel.ext.android.viewModel

class IdentitiesFragment : Fragment() {

    private val viewModel: IdentitiesViewModel by viewModel()
    private val identityAdapter = IdentityAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.recycler_view_layout, container, false)

    //TODO BaseFragment?
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        setupLiveData()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun setupRecycleView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = identityAdapter
        }
        identityAdapter.removeIdentityLiveData.observe(this, EventObserver { showRemoveDialog(it) })
    }

    private fun showError(error: Throwable) {
        Toast.makeText(this.context, error.message, Toast.LENGTH_SHORT).show()
    }

    private fun setupLiveData() {
        viewModel.walletConfigLiveData.observe(this, Observer { identityAdapter.updateList(it.identities.toMutableList()) })
        viewModel.errorLiveData.observe(this, EventObserver { showError(it) })
    }

    private fun showRemoveDialog(identity: Identity) {
        this.context?.let { context ->
            val dialog = AlertDialog.Builder(context)
                .setTitle(identity.name)
                .setMessage(R.string.remove_identity_dialog_message)
                .setPositiveButton(R.string.yes) { dialog, _ ->
                    viewModel.removeIdentity(identity)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.no) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        }
    }
}