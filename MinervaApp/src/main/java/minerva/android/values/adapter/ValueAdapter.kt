package minerva.android.values.adapter

import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.value_list_row.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.rotate180
import minerva.android.extension.rotate180back
import minerva.android.extension.visible
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.walletconfig.Network
import minerva.android.widget.getNetworkColor
import minerva.android.widget.getNetworkIcon
import minerva.wrapped.startValueAddressWrappedActivity


class ValueAdapter : RecyclerView.Adapter<ValueViewHolder>() {

    private var activeValues = mutableListOf<Value>()
    private var rawValues = listOf<Value>()

    override fun getItemCount(): Int = activeValues.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueViewHolder =
        ValueViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.value_list_row, parent,
                false
            ), parent
        )

    override fun onBindViewHolder(holder: ValueViewHolder, position: Int) {
        val rawPosition = getPositionInRaw(activeValues[position].index)
        holder.setData(rawPosition, activeValues[position])
    }

    private fun getPositionInRaw(index: Int): Int {
        rawValues.forEachIndexed { position, identity ->
            if (identity.index == index) {
                return position
            }
        }
        return Int.InvalidIndex
    }

    fun updateList(data: List<Value>) {
        rawValues = data
        activeValues.clear()
        data.forEach {
            if (!it.isDeleted) {
                activeValues.add(it)
            }
        }
        notifyDataSetChanged()
    }
}

class ValueViewHolder(private val view: View, private val viewGroup: ViewGroup) : RecyclerView.ViewHolder(view) {

    private var isOpen = false

    fun setData(rawPosition: Int, value: Value) {
        view.apply {
            card.setCardBackgroundColor(ContextCompat.getColor(context, getNetworkColor(Network.fromString(value.network))))
            icon.setImageResource(getNetworkIcon(Network.fromString(value.network)))
            valueName.text = value.name
            cryptoShortName.text = value.network
            cryptoShortName.setTextColor(ContextCompat.getColor(view.context, getNetworkColor(Network.fromString(value.network))))
            //TODO add data from block chain!
            amountView.setAmounts(12.53400f, 35.3f)
            sendButton.text = String.format(SEND_BUTTON_FORMAT, view.context.getString(R.string.send), value.network)

            sendButton.setOnClickListener {
                //TODO implement sending crypto
                Toast.makeText(view.context, "Sending ${value.network}", Toast.LENGTH_SHORT).show()
            }

            menu.setOnClickListener { showMenu(rawPosition, value.name, Network.fromString(value.network), menu) }

            setOnClickListener {
                TransitionManager.beginDelayedTransition(viewGroup)
                if (isOpen) close() else open()
            }
        }
    }

    private fun open() {
        isOpen = true
        view.apply {
            arrow.rotate180()
            sendButton.visible()
        }
    }

    private fun close() {
        isOpen = false
        view.apply {
            arrow.rotate180back()
            sendButton.gone()
        }
    }

    private fun showMenu(position: Int, title: String, network: Network, anchor: View): Boolean {
        PopupMenu(view.context, anchor).apply {
            menuInflater.inflate(R.menu.value_menu, menu)
            gravity = Gravity.RIGHT
            show()

            //TODO add rest of the menu functionality
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.showAddress -> startValueAddressWrappedActivity(view.context, position, title, getNetworkIcon(network))
                    R.id.addAsset -> Toast.makeText(view.context, "Add an asset", Toast.LENGTH_SHORT).show()
                    R.id.remove -> Toast.makeText(view.context, "Remove", Toast.LENGTH_SHORT).show()
                    else -> {
                    }
                }
                true
            }
        }
        return true
    }

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
    }
}