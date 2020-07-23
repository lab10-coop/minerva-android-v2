package minerva.android.identities.adapter

import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.identity_list_row.view.*
import minerva.android.R
import minerva.android.extension.rotate180
import minerva.android.extension.rotate180back
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.model.Identity
import minerva.android.widget.IdentityDataContent.Companion.FIELD_DESCRIPTION_LIMIT
import minerva.android.widget.repository.generateColor

class IdentityAdapter(private val listener: IdentityFragmentListener) : RecyclerView.Adapter<IdentityViewHolder>() {

    private var activeIdentities = listOf<Identity>()
    private var rawIdentities = listOf<Identity>()

    override fun getItemCount(): Int = activeIdentities.size

    override fun onBindViewHolder(holder: IdentityViewHolder, position: Int) {
        holder.setData(getPositionInRaw(activeIdentities[position].index), activeIdentities[position], isRemovable())
    }

    private fun isRemovable() = activeIdentities.size > LAST_ELEMENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdentityViewHolder =
        IdentityViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.identity_list_row, parent, false), parent, listener)

    fun updateList(data: List<Identity>) {
        rawIdentities = data
        activeIdentities = data.filter { !it.isDeleted }
        notifyDataSetChanged()
    }

    private fun getPositionInRaw(index: Int): Int {
        rawIdentities.forEachIndexed { position, identity ->
            if (identity.index == index) {
                return position
            }
        }
        return Int.InvalidIndex
    }

    companion object {
        private const val LAST_ELEMENT = 1
    }
}

class IdentityViewHolder(
    private val view: View,
    private val viewGroup: ViewGroup,
    private val listener: IdentityFragmentListener
) : RecyclerView.ViewHolder(view) {

    private val isOpen get() = view.dataContainer.isOpen
    private var removable = true

    fun setData(rawPosition: Int, identity: Identity, removable: Boolean) {
        this.removable = removable
        with(identity) {
            view.apply {
                identityName.text = name
                card.setCardBackgroundColor(ContextCompat.getColor(context, generateColor(name)))
                profileImage.createLogo(name)
                identityDid.setSingleLineTitleAndBody(Identity.DID_LABEL, did)
                dataContainer.prepareData(data)
                arrow.visibleOrGone(data.size > FIELD_DESCRIPTION_LIMIT)
                setOnClickListeners(rawPosition, identity, removable)
            }
        }
    }

    private fun View.setOnClickListeners(rawPosition: Int, identity: Identity, removable: Boolean) {
        setOnClickListener {
            TransitionManager.beginDelayedTransition(viewGroup)
            if (isOpen) close() else open()
        }
        menu.setOnClickListener { showMenu(rawPosition, identity, menu, removable) }
    }

    private fun showMenu(position: Int, identity: Identity, anchor: View, removable: Boolean) {
        PopupMenu(view.context, anchor).apply {
            menuInflater.inflate(R.menu.identity_menu, menu)
            menu.findItem(R.id.remove).isVisible = removable
            gravity = Gravity.END
            show()
            setOnItemMenuClickListener(position, identity)
        }
    }

    private fun PopupMenu.setOnItemMenuClickListener(position: Int, identity: Identity) {
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.showIdentity -> listener.showIdentity(identity)
                R.id.edit -> listener.onIdentityEdit(position, identity.name)
                R.id.remove -> listener.onIdentityRemoved(identity)
            }
            true
        }
    }

    private fun open() {
        view.apply {
            arrow.rotate180()
            dataContainer.open()
        }
    }

    private fun close() {
        view.apply {
            arrow.rotate180back()
            dataContainer.close()
        }
    }
}

interface IdentityFragmentListener {
    fun showIdentity(identity: Identity)
    fun onIdentityRemoved(identity: Identity)
    fun onIdentityEdit(position: Int, name: String)
}