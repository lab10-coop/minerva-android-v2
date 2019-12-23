package minerva.android.identities.adapter

import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.identity_list_row.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.rotate180
import minerva.android.extension.rotate180back
import minerva.android.extension.visible
import minerva.android.walletmanager.model.Identity
import minerva.android.widget.generateColor

class IdentityAdapter : RecyclerView.Adapter<IdentityViewHolder>() {

    private var identities = emptyList<Identity>()

    override fun getItemCount(): Int = identities.size

    override fun onBindViewHolder(holder: IdentityViewHolder, position: Int) {
        holder.setData(identities[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdentityViewHolder =
        IdentityViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.identity_list_row, parent,
                false
            ), parent
        )

    fun updateList(data: List<Identity>) {
        identities = data
        notifyDataSetChanged()
    }
}

class IdentityViewHolder(private val view: View, private val viewGroup: ViewGroup) : RecyclerView.ViewHolder(view) {

    private var isOpen = false

    fun setData(identity: Identity) {
        view.apply {
            identityName.text = identity.name
            card.setCardBackgroundColor(ContextCompat.getColor(context, generateColor(identity.name)))
            profileImage.createLogo(identity.name)

            setOnClickListener {
                TransitionManager.beginDelayedTransition(viewGroup)
                if (isOpen) close()
                else open(identity.isRemovable)
            }

            dataContainer.prepareData(identity.data)

            editButton.setOnClickListener {
                //TODO implement button listener in Fragment
                Toast.makeText(context, "Edit button tapped!", Toast.LENGTH_SHORT).show()
            }
            removeButton.setOnClickListener {
                //TODO implement button listener in Fragment
                Toast.makeText(context, "Remove button tapped!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun open(removable: Boolean) {
        isOpen = true
        view.apply {
            arrow.rotate180()
            dataContainer.open()
            editButton.visible()
            if (removable) removeButton.visible()
        }
    }

    private fun close() {
        isOpen = false
        view.apply {
            arrow.rotate180back()
            dataContainer.close()
            editButton.gone()
            removeButton.gone()
        }
    }
}