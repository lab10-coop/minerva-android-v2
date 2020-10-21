package minerva.android.minervaPrimitive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recycler_view_layout.*
import minerva.android.R
import minerva.android.kotlinUtils.Empty
import minerva.android.main.base.BaseFragment
import minerva.android.services.listener.MinervaPrimitiveClickListener
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.walletmanager.model.Service

abstract class MinervaPrimitiveListFragment : BaseFragment(), MinervaPrimitiveClickListener {

    lateinit var primitivesAdapter: MinervaPrimitiveAdapter
    abstract fun prepareObservers()

    open fun onRemoveCredential(credential: Credential) {}
    open fun onRemoveService(service: Service) {}
    open fun onCredentialContainerClick(credential: Credential) {}
    open fun getLoggedIdentityName(loggedInIdentityDid: String): String = String.Empty

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.recycler_view_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycleView()
        prepareObservers()
    }

    private fun setupRecycleView() {
        primitivesAdapter = MinervaPrimitiveAdapter(this)
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

    override fun onContainerClick(minervaPrimitive: MinervaPrimitive) {
        when (minervaPrimitive) {
            is Credential -> onCredentialContainerClick(minervaPrimitive)
        }
    }

    override fun getLoggedIdentityName(minervaPrimitive: MinervaPrimitive): String =
        when (minervaPrimitive) {
            is Credential -> getLoggedIdentityName(minervaPrimitive.loggedInIdentityDid)
            else -> String.Empty
        }
}