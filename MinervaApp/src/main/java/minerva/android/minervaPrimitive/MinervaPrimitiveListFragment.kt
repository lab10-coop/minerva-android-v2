package minerva.android.minervaPrimitive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.services.listener.MinervaPrimitiveMenuListener
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.walletmanager.model.Service

abstract class MinervaPrimitiveListFragment() : Fragment(), MinervaPrimitiveMenuListener {

    val primitivesAdapter = MinervaPrimitiveAdapter(this)
    abstract fun prepareObservers()

    open fun onRemoveCredential(credential: Credential) {}
    open fun onRemoveService(service: Service) {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        prepareObservers()
    }

    private fun setupRecycleView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = primitivesAdapter
        }
    }

    override fun onRemoved(minervaPrimitive: MinervaPrimitive) {
        when (minervaPrimitive) {
            is Service -> onRemoveService(minervaPrimitive)
            is Credential -> onRemoveCredential(minervaPrimitive)
        }
    }
}