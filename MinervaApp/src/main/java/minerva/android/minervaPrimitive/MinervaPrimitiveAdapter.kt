package minerva.android.minervaPrimitive

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import minerva.android.R
import minerva.android.databinding.MinervaPrimitiveListRowBinding
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.extensions.loadImageUrl
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.services.listener.MinervaPrimitiveClickListener
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.DappSessionV1
import minerva.android.walletmanager.model.walletconnect.DappSessionV2
import minerva.android.walletmanager.model.walletconnect.Pairing
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepositoryImpl
import minerva.android.walletmanager.utils.AddressConverter.getShortAddress
import minerva.android.walletmanager.utils.AddressType

class MinervaPrimitiveAdapter(private val listener: MinervaPrimitiveClickListener) :
    RecyclerView.Adapter<MinervaPrimitiveViewHolder>() {

    private var primitives: List<MinervaPrimitive> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MinervaPrimitiveViewHolder =
        MinervaPrimitiveViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.minerva_primitive_list_row, parent, false), listener
        )

    override fun getItemCount(): Int = primitives.size

    override fun onBindViewHolder(holder: MinervaPrimitiveViewHolder, position: Int) {
        holder.bindData(primitives[position])
    }

    fun updateList(primitives: List<MinervaPrimitive>) {
        this.primitives = primitives
        notifyDataSetChanged()
    }
}

class MinervaPrimitiveViewHolder(
    private val view: View,
    private val listener: MinervaPrimitiveClickListener
) : RecyclerView.ViewHolder(view) {

    private var binding = MinervaPrimitiveListRowBinding.bind(view)

    @SuppressLint("SetTextI18n")
    fun bindData(minervaPrimitive: MinervaPrimitive) {
        binding.apply {

            when (minervaPrimitive) {
                is Credential -> showCredential(minervaPrimitive, binding)
                is Service -> showService(minervaPrimitive, binding)
                is DappSessionV1 -> showDappSessionsV1(minervaPrimitive, binding)
                is DappSessionV2 -> showDappSessionsV2(minervaPrimitive, binding)
                is Pairing -> showPairing(minervaPrimitive, binding)
            }
            minervaPrimitiveName.text = minervaPrimitive.name
            setupPopupMenu(minervaPrimitive, binding)
        }
    }

    private fun showService(minervaPrimitive: Service, binding: MinervaPrimitiveListRowBinding) = with(binding) {
        setSessionItemsVisibility(false)
        binding.minervaPrimitiveLogo.loadImageUrl(minervaPrimitive.iconUrl, R.drawable.ic_services)
        showLastUsed(minervaPrimitive, binding)
    }

    private fun showDappSessionsV1(minervaPrimitive: DappSessionV1, binding: MinervaPrimitiveListRowBinding) = with(binding) {
        Glide.with(root.context)
            .load(minervaPrimitive.iconUrl)
            .error(R.drawable.ic_services)
            .into(minervaPrimitiveLogo)
        setSessionItemsVisibility(true)
        sessionInfoLabel.text = "${minervaPrimitive.accountName}: ${minervaPrimitive.address}"
        networkLabel.text = minervaPrimitive.networkName
    }

    // todo: show different things for version 2
    private fun showDappSessionsV2(minervaPrimitive: DappSessionV2, binding: MinervaPrimitiveListRowBinding) = with(binding) {
        Glide.with(root.context)
            .load(minervaPrimitive.iconUrl)
            .error(R.drawable.ic_services)
            .into(minervaPrimitiveLogo)
        setSessionItemsVisibility(true)
        // todo: #1 or #2 or etc.
        val namespaces = minervaPrimitive.namespaces ?: emptyMap()
        sessionInfoLabel.text = WalletConnectRepositoryImpl
            .namespacesToAddresses(namespaces)
            .joinToString(" • ") { getShortAddress(AddressType.NORMAL_ADDRESS, it) }
        networkLabel.text = WalletConnectRepositoryImpl
            .sessionNamespacesToChainNames(namespaces)
            .joinToString(" • ")
    }

    private fun showPairing(minervaPrimitive: Pairing, binding: MinervaPrimitiveListRowBinding) = with(binding) {
        Glide.with(root.context)
            .load(minervaPrimitive.iconUrl)
            .error(R.drawable.ic_services)
            .into(minervaPrimitiveLogo)
        setSessionItemsVisibility(true)
        sessionInfoLabel.text = view.context.getString(R.string.no_active_session)
        networkLabel.text = String.Empty
    }

    private fun MinervaPrimitiveListRowBinding.setSessionItemsVisibility(isVisible: Boolean) {
        lastUsedLabel.visibleOrGone(!isVisible)
        sessionInfoLabel.visibleOrGone(isVisible)
        networkLabel.visibleOrGone(isVisible)
    }

    private fun showCredential(minervaPrimitive: MinervaPrimitive, binding: MinervaPrimitiveListRowBinding) =
        with(binding) {
            minervaPrimitiveLogo.loadImageUrl(minervaPrimitive.iconUrl, R.drawable.ic_default_credential)
            container.setOnClickListener { listener.onContainerClick(minervaPrimitive) }
            showLastUsed(minervaPrimitive, binding)
            identityName.apply {
                visible()
                listener.getLoggedIdentityName(minervaPrimitive).let { identityName ->
                    text = String.format(context.getString(R.string.identity_label, identityName))
                }
            }
        }

    @SuppressLint("RestrictedApi")
    private fun setupPopupMenu(minervaPrimitive: MinervaPrimitive, binding: MinervaPrimitiveListRowBinding) {
        binding.popupMenu.setOnClickListener { view ->
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.remove_menu, menu)
                setMenuItems(minervaPrimitive)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.remove, R.id.disconnect -> listener.onRemoved(minervaPrimitive)
                        R.id.change_account -> listener.onChangeAccount(minervaPrimitive)
                        R.id.end_session -> listener.onEndSession(minervaPrimitive)
                    }
                    true
                }
            }.also {
                with(MenuPopupHelper(view.context, it.menu as MenuBuilder, binding.popupMenu)) {
                    setForceShowIcon(true)
                    gravity = Gravity.END
                    show()
                }
            }
        }
    }

    private fun PopupMenu.setMenuItems(minervaPrimitive: MinervaPrimitive) {
        with(menu) {
            findItem(R.id.disconnect).isVisible = minervaPrimitive is DappSession || minervaPrimitive is Pairing
            findItem(R.id.change_account).isVisible = minervaPrimitive is DappSessionV1
            findItem(R.id.end_session).isVisible = minervaPrimitive is DappSessionV2
            findItem(R.id.remove).isVisible = minervaPrimitive is Service || minervaPrimitive is Credential
        }
    }

    private fun showLastUsed(minervaPrimitive: MinervaPrimitive, binding: MinervaPrimitiveListRowBinding) {
        binding.lastUsedLabel.text =
            "${view.context.getString(R.string.last_used)} ${DateUtils.getDateWithTimeFromTimestamp(minervaPrimitive.lastUsed)}"
    }
}