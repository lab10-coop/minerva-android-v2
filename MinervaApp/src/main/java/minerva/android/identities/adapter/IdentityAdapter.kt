package minerva.android.identities.adapter

import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.identity_list_row.view.*
import minerva.android.R
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.Identity
import minerva.android.widget.repository.generateColor
import minerva.android.wrapped.startEditIdentityWrappedActivity

class IdentityAdapter : RecyclerView.Adapter<IdentityViewHolder>() {

    private var activeIdentities = listOf<Identity>()
    private var rawIdentities = listOf<Identity>()

    private val _removeIdentityLiveData = MutableLiveData<Event<Identity>>()
    val removeIdentityLiveData: LiveData<Event<Identity>> get() = _removeIdentityLiveData

    override fun getItemCount(): Int = activeIdentities.size

    override fun onBindViewHolder(holder: IdentityViewHolder, position: Int) {
        val rawPosition = getPositionInRaw(activeIdentities[position].index)
        holder.setData(rawPosition, activeIdentities[position], _removeIdentityLiveData, activeIdentities.size > LAST_ELEMENT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdentityViewHolder =
        IdentityViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.identity_list_row, parent,
                false
            ), parent
        )

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

class IdentityViewHolder(private val view: View, private val viewGroup: ViewGroup) : RecyclerView.ViewHolder(view) {

    private val isOpen get() = view.editButton.isVisible
    private var removable = true

    fun setData(rawPosition: Int, identity: Identity, removeIdentityLiveData: MutableLiveData<Event<Identity>>, removable: Boolean) {
        updateRemoveButton(removable)
        view.apply {
            identityName.text = identity.name
            card.setCardBackgroundColor(ContextCompat.getColor(context, generateColor(identity.name)))
            profileImage.createLogo(identity.name)
            dataContainer.prepareData(identity.data)

            setOnClickListener {
                TransitionManager.beginDelayedTransition(viewGroup)
                if (isOpen) close() else open()
            }
            editButton.setOnClickListener { startEditIdentityWrappedActivity(view.context, rawPosition, identity.name) }
            removeButton.setOnClickListener { removeIdentityLiveData.value = Event(identity) }
        }
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