package minerva.android.minervaPrimitive

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.minerva_primitive_list_row.view.*
import minerva.android.R
import minerva.android.extension.visible
import minerva.android.extensions.loadImageUrl
import minerva.android.services.listener.MinervaPrimitiveClickListener
import minerva.android.walletmanager.model.Credential
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.widget.repository.getServiceIcon

class MinervaPrimitiveAdapter(private val listener: MinervaPrimitiveClickListener) : RecyclerView.Adapter<MinervaPrimitiveViewHolder>() {

    private var primitives: List<MinervaPrimitive> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MinervaPrimitiveViewHolder =
        MinervaPrimitiveViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.minerva_primitive_list_row, parent, false), parent, listener
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
    private val viewGroup: ViewGroup,
    private val listener: MinervaPrimitiveClickListener
) : RecyclerView.ViewHolder(view) {

    @SuppressLint("SetTextI18n")
    fun bindData(minervaPrimitive: MinervaPrimitive) {
        view.apply {

            when (minervaPrimitive) {
                is Credential -> {
                    minervaPrimitiveLogo.loadImageUrl(minervaPrimitive.iconUrl)
                    identityName.apply {
                        visible()
                        text = String.format(context.getString(R.string.identity_label, minervaPrimitive.memberName))
                    }
                }
                is Service -> showIcon(getServiceIcon(minervaPrimitive.type))
            }

            minervaPrimitiveName.text = minervaPrimitive.name
            lastUsedLabel.text = "${context.getString(R.string.last_used)} ${DateUtils.getDateWithTimeFromTimestamp(minervaPrimitive.lastUsed)}"
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

    private fun View.showIcon(icon: Int) {
        Glide.with(viewGroup.context).load(icon).into(minervaPrimitiveLogo)
    }
}