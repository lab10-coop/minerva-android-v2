package minerva.android.minervaPrimitive

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.MinervaPrimitiveListRowBinding
import minerva.android.extension.visible
import minerva.android.extensions.loadImageUrl
import minerva.android.kotlinUtils.DateUtils
import minerva.android.services.listener.MinervaPrimitiveClickListener
import minerva.android.walletmanager.model.Credential
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

    val context: Context
        get() = view.context

    @SuppressLint("SetTextI18n")
    fun bindData(minervaPrimitive: MinervaPrimitive) {
        binding.apply {

            when (minervaPrimitive) {
                is Credential -> {
                    minervaPrimitiveLogo.loadImageUrl(minervaPrimitive.iconUrl, R.drawable.ic_default_credential)
                    identityName.apply {
                        visible()
                        listener.getLoggedIdentityName(minervaPrimitive).let { identityName ->
                            text = String.format(context.getString(R.string.identity_label, identityName))
                        }
                    }
                }
                is Service -> minervaPrimitiveLogo.loadImageUrl(minervaPrimitive.iconUrl, R.drawable.ic_services)
            }

            minervaPrimitiveName.text = minervaPrimitive.name
            lastUsedLabel.text =
                "${context.getString(R.string.last_used)} ${DateUtils.getDateWithTimeFromTimestamp(minervaPrimitive.lastUsed)}"
            container.setOnClickListener { listener.onContainerClick(minervaPrimitive) }
            popupMenu.setOnClickListener { view ->
                PopupMenu(this@MinervaPrimitiveViewHolder.view.context, view).apply {
                    menuInflater.inflate(R.menu.remove_menu, menu)
                    gravity = Gravity.END
                    show()
                    setOnMenuItemClickListener {
                        if (it.itemId == R.id.remove) listener.onRemoved(minervaPrimitive)
                        true
                    }
                }
            }
        }
    }
}