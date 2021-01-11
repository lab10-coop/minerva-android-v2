package minerva.android.minervaPrimitive

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.databinding.RecyclerViewLayoutBinding
import minerva.android.kotlinUtils.Empty
import minerva.android.main.base.BaseFragment
import minerva.android.services.listener.MinervaPrimitiveClickListener
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.walletmanager.model.Service

abstract class MinervaPrimitiveListFragment : BaseFragment(R.layout.recycler_view_layout), MinervaPrimitiveClickListener {

    internal lateinit var binding: RecyclerViewLayoutBinding
    lateinit var primitivesAdapter: MinervaPrimitiveAdapter
    abstract fun prepareObservers()

    open fun onRemoveCredential(credential: Credential) {}
    open fun onRemoveService(service: Service) {}
    open fun onCredentialContainerClick(credential: Credential) {}
    open fun getLoggedIdentityName(loggedInIdentityDid: String): String = String.Empty

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = RecyclerViewLayoutBinding.bind(view)
        setupRecycleView()
        prepareObservers()
    }

    private fun setupRecycleView() {
        primitivesAdapter = MinervaPrimitiveAdapter(this)
        binding.recyclerView.apply {
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