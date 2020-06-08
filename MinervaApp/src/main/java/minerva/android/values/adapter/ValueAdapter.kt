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
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.values.listener.ValuesFragmentToAdapterListener
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.Value
import minerva.android.widget.AssetView
import minerva.android.utils.BalanceUtils.getCryptoBalance
import minerva.android.utils.BalanceUtils.getFiatBalance
import minerva.android.widget.repository.getNetworkColor
import minerva.android.widget.repository.getNetworkIcon
import minerva.android.wrapped.startSafeAccountWrappedActivity
import minerva.android.wrapped.startValueAddressWrappedActivity

class ValueAdapter(private val listener: ValuesFragmentToAdapterListener) :
    RecyclerView.Adapter<ValueViewHolder>(),
    ValueViewHolder.ValuesAdapterListener {

    private var activeValues = listOf<Value>()
    private var rawValues = listOf<Value>()

    override fun getItemCount(): Int = activeValues.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueViewHolder =
        ValueViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.value_list_row, parent, false), parent)

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
                if (cryptoBalance != balances[address]?.cryptoBalance) {
                    cryptoBalance = balances[address]?.cryptoBalance ?: Int.InvalidId.toBigDecimal()
                    fiatBalance = balances[address]?.fiatBalance ?: Int.InvalidId.toBigDecimal()
                    notifyItemChanged(index)
                }
            }
        }
    }

    fun updateAssetBalances(assetBalances: Map<String, List<Asset>>) {
        activeValues.forEach { value -> assetBalances[value.privateKey]?.let { value.assets = it } }
    }

    override fun onSendValueClicked(value: Value) = listener.onSendTransaction(value)

    override fun onSendAssetClicked(valueIndex: Int, assetIndex: Int) = listener.onSendAssetTransaction(valueIndex, assetIndex)

    override fun onValueRemoved(position: Int) = listener.onValueRemove(rawValues[position])

    override fun refreshAssets(rawPosition: Int): List<Asset> = rawValues[rawPosition].assets

    override fun onCreateSafeAccountClicked(value: Value) = listener.onCreateSafeAccount(value)
}

class ValueViewHolder(private val view: View, private val viewGroup: ViewGroup) : AssetView.AssertViewCallback, RecyclerView.ViewHolder(view) {

    private lateinit var listener: ValuesAdapterListener
    private var rawPosition: Int = Int.InvalidIndex

    private val isOpen get() = view.sendButton.isVisible

    override fun onSendAssetClicked(valueIndex: Int, assetIndex: Int) = listener.onSendAssetClicked(valueIndex, assetIndex)

    override fun getViewGroup() = viewGroup

    override fun getContext(): Context = view.context

    fun setListener(listener: ValuesAdapterListener) {
        this.listener = listener
    }

    fun setData(rawPosition: Int, value: Value) {
        this.rawPosition = rawPosition
        view.apply {
            bindData(value)
            prepareView(value)
            prepareAssets(value)
            setOnSendButtonClickListener(value)
            setOnMenuClickListener(rawPosition, value)
            setOnItemClickListener()
        }
    }

    private fun View.bindData(value: Value) {
        with(value) {
            card.setCardBackgroundColor(ContextCompat.getColor(context, getNetworkColor(Network.fromString(network))))
            icon.setImageResource(getNetworkIcon(Network.fromString(network)))
            valueName.text = name
            cryptoShortName.run {
                text = network
                setTextColor(ContextCompat.getColor(context, getNetworkColor(Network.fromString(network))))
            }
            with(amountView) {
                setCrypto(getCryptoBalance(cryptoBalance))
                setFiat(getFiatBalance(fiatBalance))
            }
            sendButton.text = String.format(SEND_BUTTON_FORMAT, view.context.getString(R.string.send), network)
        }
    }

    private fun View.prepareView(value: Value) {
        if (!value.isSafeAccount) {
            prepareView()
        } else {
            prepareSafeAccountView()
        }
    }

    private fun View.prepareView() {
        mainContent.run {
            margin(NO_FRAME, FRAME_TOP_WIDTH, NO_FRAME, NO_FRAME)
            setBackgroundResource(R.drawable.identity_background)
        }
        safeAccountBadge.gone()
    }

    private fun View.prepareSafeAccountView() {
        mainContent.run {
            margin(FRAME_WIDTH, FRAME_TOP_WIDTH, FRAME_WIDTH, FRAME_WIDTH)
            setBackgroundResource(R.drawable.safe_account_background)
        }
        safeAccountBadge.visible()
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

    private fun View.prepareAssets(value: Value) {
        container.removeAllViews()
        value.assets.forEachIndexed { index, asset ->
            container.addView(AssetView(this@ValueViewHolder, value, index, R.drawable.ic_asset_sdai).apply {
                setAmounts(asset.balance)
            })
        }
    }

    private fun open() {
        TransitionManager.beginDelayedTransition(viewGroup)
        view.apply {
            arrow.rotate180()
            sendButton.visible()
            container.visible()
        }
    }

    private fun close() {
        TransitionManager.endTransitions(viewGroup)
        TransitionManager.beginDelayedTransition(viewGroup)
        view.apply {
            arrow.rotate180back()
            sendButton.gone()
            container.gone()
        }
    }

    private fun showMenu(position: Int, value: Value, anchor: View): Boolean {
        PopupMenu(view.context, anchor).apply {
            menuInflater.inflate(R.menu.value_menu, menu)
            menu.findItem(R.id.addSafeAccount).isVisible = isCreatingSafeAccountAvailable(value)
            menu.findItem(R.id.safeAccountSettings).isVisible = isSafeAccount(value)
            gravity = Gravity.END
            show()
            setOnItemMenuClickListener(position, value)
        }
        return true
    }

    private fun PopupMenu.setOnItemMenuClickListener(
        position: Int,
        value: Value
    ) {
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.showAddress -> startValueAddressWrappedActivity(
                    view.context, value.name, position,
                    getNetworkIcon(Network.fromString(value.network))
                )
                R.id.safeAccountSettings -> startSafeAccountWrappedActivity(
                    view.context, value.name, position,
                    R.drawable.ic_safe_account_single_owner
                )
                R.id.addSafeAccount -> listener.onCreateSafeAccountClicked(value)
                R.id.remove -> listener.onValueRemoved(position)
            }
            true
        }
    }

    private fun isCreatingSafeAccountAvailable(value: Value) = value.network == Network.ARTIS.short && !value.isSafeAccount

    private fun isSafeAccount(value: Value) = value.network == Network.ARTIS.short && value.isSafeAccount

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
        private const val FRAME_TOP_WIDTH = 3f
        private const val NO_FRAME = 0f
        private const val FRAME_WIDTH = 1.5f
    }

    interface ValuesAdapterListener {
        fun onSendValueClicked(value: Value)
        fun onSendAssetClicked(valueIndex: Int, assetIndex: Int)
        fun onValueRemoved(position: Int)
        fun refreshAssets(rawPosition: Int): List<Asset>
        fun onCreateSafeAccountClicked(value: Value)
    }
}