package minerva.android.minervaPrimitive

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import minerva.android.R
import minerva.android.accounts.walletconnect.*
import minerva.android.databinding.RecyclerViewLayoutBinding
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.main.MainActivity
import minerva.android.main.base.BaseFragment
import minerva.android.services.ServicesFragment
import minerva.android.services.listener.MinervaPrimitiveClickListener
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.walletconnect.*

abstract class MinervaPrimitiveListFragment : BaseFragment(R.layout.recycler_view_layout), MinervaPrimitiveClickListener {

    internal lateinit var binding: RecyclerViewLayoutBinding
    lateinit var primitivesAdapter: MinervaPrimitiveAdapter
    abstract fun prepareObservers()

    open fun onRemoveCredential(credential: Credential) {}
    open fun onRemoveService(service: Service) {}
    open fun onRemoveDappSession(dapp: DappSessionV1) {}
    open fun onEndDappSession(dapp: DappSessionV2) {}
    open fun onRemovePairing(dapp: MinervaPrimitive) {}
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
            is DappSessionV1 -> onRemoveDappSession(minervaPrimitive)
            is DappSessionV2, is Pairing -> onRemovePairing(minervaPrimitive)
        }
    }

    override fun onEndSession(minervaPrimitive: MinervaPrimitive) {
        when (minervaPrimitive) {
            is DappSessionV2 -> onEndDappSession(minervaPrimitive)
        }
    }

    override fun onChangeAccount(minervaPrimitive: MinervaPrimitive) {
        //trying get MainActivity instance from context - for change state through changing LiveModel
        val mainActivityFromContext: MainActivity = ((this.parentFragment as ServicesFragment).interactor as MainActivity)
        //create/set transferring current connection state (data) to caller (fragment which called action)
        val state = OnSessionRequest(
            WalletConnectPeerMeta(
                name = minervaPrimitive.name,
                icons = listOf(minervaPrimitive.iconUrl ?: ""),
                peerId = minervaPrimitive.peerId,
                address = minervaPrimitive.address,
                chainId = minervaPrimitive.chainId),
            BaseNetworkData(chainId = Int.InvalidValue, name = String.Empty), //put empties values for get popap with all available networks
            WalletConnectAlertType.CHANGE_ACCOUNT) //put value for calling wallet connection change action
        //calling LiveModel changing through pulling new state (of WalletConnectInteractionsViewModel::_walletConnectStatus)
        mainActivityFromContext.onChangeAccount(state)
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