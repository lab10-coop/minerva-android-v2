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
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.extensions.loadImageUrl
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.services.listener.MinervaPrimitiveClickListener
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.walletmanager.model.Service

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
                is Service -> {
                    minervaPrimitiveLogo.loadImageUrl(minervaPrimitive.iconUrl, R.drawable.ic_services)
                    showLastUsed(minervaPrimitive, binding)
                }
                is DappSession -> {
                    Glide.with(binding.root.context)
                        .load(minervaPrimitive.iconUrl)
                        .into(minervaPrimitiveLogo)
                    lastUsedLabel.gone()
                    identityName.gone()
                    sessionInfoLabel.visible()
                    networkLabel.visible()

                    sessionInfoLabel.text = "${minervaPrimitive.accounName}: ${minervaPrimitive.address}"
                    networkLabel.text = minervaPrimitive.networkName
                }
            }
            minervaPrimitiveName.text = minervaPrimitive.name
            setupPopupMenu(minervaPrimitive, binding)
        }
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
                    if (it.itemId == R.id.remove) listener.onRemoved(minervaPrimitive)
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
            findItem(R.id.disconnect).isVisible = minervaPrimitive is DappSession
            findItem(R.id.remove).isVisible = minervaPrimitive is Service || minervaPrimitive is Credential
        }
    }

    private fun showLastUsed(minervaPrimitive: MinervaPrimitive, binding: MinervaPrimitiveListRowBinding) {
        binding.lastUsedLabel.text =
            "${view.context.getString(R.string.last_used)} ${DateUtils.getDateWithTimeFromTimestamp(minervaPrimitive.lastUsed)}"
    }
}