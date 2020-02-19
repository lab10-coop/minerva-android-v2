package minerva.android.values.adapter

import android.content.Context
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.Value
import minerva.android.widget.AssetView
import minerva.android.widget.repository.getNetworkColor
import minerva.android.widget.repository.getNetworkIcon
import minerva.android.wrapped.startValueAddressWrappedActivity


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

    fun updateBalances(balances: HashMap<String, Balance>) {
        activeValues.forEachIndexed { index, value ->
            value.apply {
                if (balance != balances[address]?.cryptoBalance) {
                    balance = balances[address]?.cryptoBalance ?: Int.InvalidId.toBigDecimal()
                    fiatBalance = balances[address]?.fiatBalance ?: Int.InvalidId.toBigDecimal()
                    notifyItemChanged(index)
                }
            }
        }
    }

    fun updateAssetBalances(assetBalances: Map<String, List<Asset>>) {
        activeValues.forEachIndexed { index, value ->
            assetBalances[value.privateKey]?.let {
                value.assets = it
            }
        }
    }

    override fun onSendValueClicked(value: Value) = listener.onSendTransaction(value)

    override fun onSendAssetClicked() = listener.onSendAssetTransaction()

    override fun onValueRemoved(position: Int) = listener.onValueRemove(rawValues[position])

    override fun refreshAssets(rawPosition: Int): List<Asset> = rawValues[rawPosition].assets
}

class ValueViewHolder(private val view: View, private val viewGroup: ViewGroup) : AssetView.AssertViewCallback, RecyclerView.ViewHolder(view) {

    private lateinit var listener: ValuesAdapterListener
    private var rawPosition: Int = WRONG_POSITION

    private val isOpen get() = view.sendButton.isVisible

    override fun onSendAssetClicked() = listener.onSendAssetClicked()

    override fun getViewGroup() = viewGroup

    override fun getContext(): Context = view.context

    fun setListener(listener: ValuesAdapterListener) {
        this.listener = listener
    }

    fun setData(rawPosition: Int, value: Value) {
        this.rawPosition = rawPosition
        view.apply {
            bindData(value)
            setOnSendButtonClickListener(value)
            setOnMenuClickListener(rawPosition, value)
            setOnItemClickListener()
        }
    }

    private fun View.bindData(value: Value) {
        card.setCardBackgroundColor(ContextCompat.getColor(context, getNetworkColor(Network.fromString(value.network))))
        icon.setImageResource(getNetworkIcon(Network.fromString(value.network)))
        valueName.text = value.name
        cryptoShortName.text = value.network
        cryptoShortName.setTextColor(ContextCompat.getColor(context, getNetworkColor(Network.fromString(value.network))))
        amountView.setAmounts(value.balance, value.fiatBalance)
        sendButton.text = String.format(SEND_BUTTON_FORMAT, view.context.getString(R.string.send), value.network)
    }

    private fun View.setOnSendButtonClickListener(value: Value) {
        sendButton.setOnClickListener {
            listener.onSendValueClicked(value)
        }
    }

    private fun View.setOnMenuClickListener(rawPosition: Int, value: Value) {
        menu.setOnClickListener { showMenu(rawPosition, value, menu) }
    }

    private fun View.setOnItemClickListener() {
        setOnClickListener {
            if (isOpen) close() else open()
        }
    }

    private fun View.prepareAssets(assets: List<Asset>) {
        assets.forEach {
            //TODO add correct icon!
            container.addView(AssetView(this@ValueViewHolder, it.name, R.drawable.ic_asset_sdai).apply {
                setAmounts(it.balance)
            })
        }
    }

    private fun open() {
        TransitionManager.beginDelayedTransition(viewGroup)
        view.apply {
            arrow.rotate180()
            sendButton.visible()
            container.visible()
            prepareAssets(listener.refreshAssets(rawPosition))
        }
    }

    private fun close() {
        TransitionManager.endTransitions(viewGroup)
        TransitionManager.beginDelayedTransition(viewGroup)
        view.apply {
            arrow.rotate180back()
            sendButton.gone()
            container.gone()
            container.removeAllViews()
        }
    }

    private fun showMenu(position: Int, value: Value, anchor: View): Boolean {
        PopupMenu(view.context, anchor).apply {
            menuInflater.inflate(R.menu.value_menu, menu)
            gravity = Gravity.RIGHT
            show()
            setOnItemMenuClickListener(position, value)
        }
        return true
    }

    private fun PopupMenu.setOnItemMenuClickListener(
        position: Int,
        value: Value
    ) {
        //TODO add rest of the menu functionality
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.showAddress -> startValueAddressWrappedActivity(
                    view.context, position, value.name,
                    getNetworkIcon(Network.fromString(value.network))
                )
                R.id.remove -> listener.onValueRemoved(position)
            }
            true
        }
    }

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
        private const val WRONG_POSITION = -1
    }

    interface ValuesAdapterListener {
        fun onSendValueClicked(value: Value)
        fun onSendAssetClicked()
        fun onValueRemoved(position: Int)
        fun refreshAssets(rawPosition: Int): List<Asset>
    }
}