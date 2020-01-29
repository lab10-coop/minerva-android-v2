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
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.values.listener.ValuesFragmentToAdapterListener
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.walletconfig.Network
import minerva.android.widget.CryptoAmountView.Companion.WRONG_CURRENCY_VALUE
import minerva.android.widget.repository.getNetworkColor
import minerva.android.widget.repository.getNetworkIcon
import minerva.wrapped.startValueAddressWrappedActivity
import java.math.BigDecimal


class ValueAdapter(private val listener: ValuesFragmentToAdapterListener) :
    RecyclerView.Adapter<ValueViewHolder>(),
    ValueViewHolder.ValuesAdapterListener {

    private var activeValues = listOf<Value>()
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
        activeValues[position].let {
            val rawPosition = getPositionInRaw(it.index)
            holder.apply {
                setData(rawPosition, it)
                setListener(this@ValueAdapter)
            }
        }
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
        activeValues = data.filter { !it.isDeleted }
        notifyDataSetChanged()
    }

    fun updateBalances(balances: HashMap<String, BigDecimal>) {
        activeValues.forEach {
            it.balance = balances[it.address] ?: Int.InvalidId.toBigDecimal() }
        notifyDataSetChanged()
    }

    override fun onSendValueClicked(value: Value) {
        listener.onSendTransaction(value)
    }
}

class ValueViewHolder(private val view: View, private val viewGroup: ViewGroup) : RecyclerView.ViewHolder(view) {

    private var isOpen = false
    private lateinit var listener: ValuesAdapterListener

    fun setListener(listener: ValuesAdapterListener) {
        this.listener = listener
    }

    fun setData(rawPosition: Int, value: Value) {
        view.apply {
            bindData(value)
            setOnSendButtonClickListener(value)
            setOnMenuClickListener(rawPosition, value)
            setOnItemClickListener()
        }
    }

    private fun View.bindData(value: Value) {
        card.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                getNetworkColor(Network.fromString(value.network))
            )
        )
        icon.setImageResource(getNetworkIcon(Network.fromString(value.network)))
        valueName.text = value.name
        cryptoShortName.text = value.network
        cryptoShortName.setTextColor(
            ContextCompat.getColor(
                view.context,
                getNetworkColor(Network.fromString(value.network))
            )
        )
        //TODO add data for normal currency!
        amountView.setAmounts(value.balance, WRONG_CURRENCY_VALUE)
        sendButton.text = String.format(SEND_BUTTON_FORMAT, view.context.getString(R.string.send), value.network)
    }

    private fun View.setOnSendButtonClickListener(value: Value) {
        sendButton.setOnClickListener {
            listener.onSendValueClicked(value)
        }
    }

    private fun View.setOnMenuClickListener(rawPosition: Int, value: Value) {
        menu.setOnClickListener { showMenu(rawPosition, value.name, Network.fromString(value.network), menu) }
    }

    private fun View.setOnItemClickListener() {
        setOnClickListener {
            TransitionManager.beginDelayedTransition(viewGroup)
            if (isOpen) close() else open()
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
            setOnItemMenuClickListener(position, title, network)
        }
        return true
    }

    private fun PopupMenu.setOnItemMenuClickListener(
        position: Int,
        title: String,
        network: Network
    ) {
        //TODO add rest of the menu functionality
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.showAddress -> startValueAddressWrappedActivity(
                    view.context, position, title,
                    getNetworkIcon(network)
                )
                R.id.addAsset -> Toast.makeText(view.context, "Add an asset", Toast.LENGTH_SHORT).show()
                R.id.remove -> Toast.makeText(view.context, "Remove", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
    }

    interface ValuesAdapterListener {
        fun onSendValueClicked(value: Value)
    }
}