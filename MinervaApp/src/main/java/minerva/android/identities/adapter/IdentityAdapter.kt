package minerva.android.identities.adapter

import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.identity_list_row.view.*
import minerva.android.R
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.model.Identity
import minerva.android.widget.repository.generateColor
import minerva.android.wrapped.startEditIdentityWrappedActivity

class IdentityAdapter(private val listener: IdentityFragmentListener) : RecyclerView.Adapter<IdentityViewHolder>() {

    private var activeIdentities = listOf<Identity>()
    private var rawIdentities = listOf<Identity>()

    override fun getItemCount(): Int = activeIdentities.size

    override fun onBindViewHolder(holder: IdentityViewHolder, position: Int) {
        holder.setData(getPositionInRaw(activeIdentities[position].index), activeIdentities[position], activeIdentities.size > LAST_ELEMENT)
    }

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

class IdentityViewHolder(private val view: View, private val viewGroup: ViewGroup, private val listener: IdentityFragmentListener) :
    RecyclerView.ViewHolder(view) {

    private val isOpen get() = view.editButton.isVisible
    private var removable = true

    fun setData(rawPosition: Int, identity: Identity, removable: Boolean) {
        updateRemoveButton(removable)
        with(identity) {
            view.apply {
                identityName.text = name
                card.setCardBackgroundColor(ContextCompat.getColor(context, generateColor(name)))
                profileImage.createLogo(name)
                dataContainer.prepareData(data)
                setOnClickListeners(rawPosition, identity)
            }
        }
    }

    private fun View.setOnClickListeners(rawPosition: Int, identity: Identity) {
        setOnClickListener {
            TransitionManager.beginDelayedTransition(viewGroup)
            if (isOpen) close() else open()
        }
        editButton.setOnClickListener { startEditIdentityWrappedActivity(view.context, rawPosition, identity.name) }
        removeButton.setOnClickListener { listener.onIdentityRemoved(identity) }
    }

    private fun open() {
        view.apply {
            arrow.rotate180()
            dataContainer.open()
            editButton.visible()
            removeButton.visibleOrGone(removable)
        }
    }

    private fun close() {
        view.apply {
            arrow.rotate180back()
            dataContainer.close()
            editButton.gone()
            removeButton.gone()
        }
    }

    private fun updateRemoveButton(removable: Boolean) {
        this.removable = removable
        view.apply {
            dataContainer.close()
            editButton.gone()
            removeButton.visibleOrGone(removable && isOpen)
        }
    }
}

interface IdentityFragmentListener {
    fun onIdentityRemoved(identity: Identity)
}