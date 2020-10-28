package minerva.android.accounts.adapter

import android.content.Context
import android.graphics.Color
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.account_list_row.view.*
import minerva.android.R
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.utils.BalanceUtils.getCryptoBalance
import minerva.android.utils.BalanceUtils.getFiatBalance
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.AccountAsset
import minerva.android.walletmanager.model.Balance
import minerva.android.widget.AssetView
import minerva.android.widget.repository.getNetworkIcon
import java.math.BigDecimal

class AccountAdapter(private val listener: AccountsFragmentToAdapterListener) : RecyclerView.Adapter<AccountViewHolder>(),
    AccountViewHolder.AccountsAdapterListener {

    private var activeAccounts = listOf<Account>()
    private var rawAccounts = listOf<Account>()

    override fun getItemCount(): Int = activeAccounts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder =
        AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.account_list_row, parent, false), parent)

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        activeAccounts[position].let {
            val rawPosition = getPositionInRaw(it.index)
            holder.apply {
                setData(rawPosition, it)
                setListener(this@AccountAdapter)
            }
        }
    }

    fun updateList(data: List<Account>) {
        rawAccounts = data
        activeAccounts = data.filter { !it.isDeleted }
        notifyDataSetChanged()
    }

    fun updateBalances(balances: HashMap<String, Balance>) {
        activeAccounts.forEachIndexed { index, account ->
            account.apply {
                cryptoBalance = balances[address]?.cryptoBalance ?: Int.InvalidId.toBigDecimal()
                fiatBalance = balances[address]?.fiatBalance ?: Int.InvalidId.toBigDecimal()
                notifyItemChanged(index)
            }
        }
    }

    fun updateAssetBalances(accountAssetBalances: Map<String, List<AccountAsset>>) {
        activeAccounts.forEach { account -> accountAssetBalances[account.privateKey]?.let { account.accountAssets = it } }
    }

    fun setPending(index: Int, isPending: Boolean) {
        rawAccounts.forEachIndexed { position, account ->
            if (account.index == index) {
                account.isPending = isPending
                notifyItemChanged(position)
            }
        }
    }

    fun stopPendingTransactions() {
        rawAccounts.forEach {
            it.isPending = false
        }
        notifyDataSetChanged()
    }

    private fun getPositionInRaw(index: Int): Int {
        rawAccounts.forEachIndexed { position, identity ->
            if (identity.index == index) {
                return position
            }
        }
        return Int.InvalidIndex
    }

    override fun onSendAccountClicked(account: Account) {
        listener.onSendTransaction(account)
    }

    override fun onSendAssetClicked(accountIndex: Int, assetIndex: Int) {
        listener.onSendAssetTransaction(accountIndex, assetIndex)
    }

    override fun onAccountRemoved(position: Int) {
        listener.onAccountRemove(rawAccounts[position])
    }

    override fun onCreateSafeAccountClicked(account: Account) {
        listener.onCreateSafeAccount(account)
    }

    override fun onShowAddress(account: Account, position: Int) {
        listener.onShowAddress(account, position)
    }

    override fun onShowSafeAccountSettings(account: Account, position: Int) {
        listener.onShowSafeAccountSettings(account, position)
    }
}

class AccountViewHolder(private val view: View, private val viewGroup: ViewGroup) : AssetView.AssertViewCallback,
    RecyclerView.ViewHolder(view) {

    private lateinit var listener: AccountsAdapterListener
    private var rawPosition: Int = Int.InvalidIndex

    private val isOpen get() = view.sendButton.isVisible

    override fun onSendAssetClicked(accountIndex: Int, assetIndex: Int) = listener.onSendAssetClicked(accountIndex, assetIndex)

    override fun getViewGroup() = viewGroup

    override fun getContext(): Context = view.context

    fun setListener(listener: AccountsAdapterListener) {
        this.listener = listener
    }

    fun setData(rawPosition: Int, account: Account) {
        this.rawPosition = rawPosition
        view.apply {
            bindData(account)
            prepareView(account)
            prepareAssets(account)
            setOnSendButtonClickListener(account)
            setOnMenuClickListener(rawPosition, account)
            setOnItemClickListener()
        }
    }

    private fun View.bindData(account: Account) {
        with(account) {
            card.setCardBackgroundColor(Color.parseColor(NetworkManager.getStringColor(network, isPending)))
            progress.apply {
                visibleOrGone(isPending)
                DrawableCompat.setTint(indeterminateDrawable, Color.parseColor(network.color))
            }
            pendingMask.visibleOrGone(isPending)
            icon.setImageDrawable(getNetworkIcon(context, network.short, isSafeAccount))
            accountName.text = name
            cryptoTokenName.run {
                text = network.token
                setTextColor(Color.parseColor(network.color))
            }
            with(amountView) {
                setCrypto(getCryptoBalance(cryptoBalance))
                (if (network.testnet) BigDecimal.ZERO
                else account.fiatBalance).let { setFiat(getFiatBalance(it)) }
            }
            sendButton.text = String.format(SEND_BUTTON_FORMAT, view.context.getString(R.string.send), network.token)
        }
    }

    private fun View.prepareView(account: Account) {
        if (!account.isSafeAccount) {
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
    }

    private fun View.prepareSafeAccountView() {
        mainContent.run {
            margin(FRAME_WIDTH, FRAME_TOP_WIDTH, FRAME_WIDTH, FRAME_WIDTH)
            setBackgroundResource(R.drawable.safe_account_background)
        }
    }

    private fun View.setOnSendButtonClickListener(account: Account) {
        sendButton.setOnClickListener {
            listener.onSendAccountClicked(account)
        }
    }

    private fun View.setOnMenuClickListener(rawPosition: Int, account: Account) {
        menu.setOnClickListener { showMenu(rawPosition, account, menu) }
    }

    private fun View.setOnItemClickListener() {
        setOnClickListener {
            if (isOpen) close() else open()
        }
    }

    private fun View.prepareAssets(account: Account) {
        container.removeAllViews()
        account.accountAssets.forEachIndexed { index, asset ->
            container.addView(AssetView(this@AccountViewHolder, account, index, R.drawable.ic_asset_sdai).apply {
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

    private fun showMenu(position: Int, account: Account, anchor: View) {
        PopupMenu(view.context, anchor).apply {
            menuInflater.inflate(R.menu.account_menu, menu)
            menu.findItem(R.id.addSafeAccount).isVisible = isCreatingSafeAccountAvailable(account)
            menu.findItem(R.id.safeAccountSettings).isVisible = isSafeAccount(account)
            gravity = Gravity.END
            show()
            setOnItemMenuClickListener(position, account)
        }
    }

    private fun PopupMenu.setOnItemMenuClickListener(position: Int, account: Account) {
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.showAddress -> listener.onShowAddress(account, position)
                R.id.safeAccountSettings -> listener.onShowSafeAccountSettings(account, position)
                R.id.addSafeAccount -> listener.onCreateSafeAccountClicked(account)
                R.id.remove -> listener.onAccountRemoved(position)
            }
            true
        }
    }

    private fun isCreatingSafeAccountAvailable(account: Account) = !isSafeAccount(account) && account.network.isSafeAccountAvailable

    private fun isSafeAccount(account: Account) = account.network.isAvailable() && account.isSafeAccount

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
        private const val FRAME_TOP_WIDTH = 3f
        private const val NO_FRAME = 0f
        private const val FRAME_WIDTH = 1.5f
    }

    interface AccountsAdapterListener {
        fun onSendAccountClicked(account: Account)
        fun onSendAssetClicked(accountIndex: Int, assetIndex: Int)
        fun onAccountRemoved(position: Int)
        fun onCreateSafeAccountClicked(account: Account)
        fun onShowAddress(account: Account, position: Int)
        fun onShowSafeAccountSettings(account: Account, position: Int)
    }
}