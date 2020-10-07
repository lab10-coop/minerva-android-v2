package minerva.android.accounts.adapter

import android.content.Context
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
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.utils.BalanceUtils.getCryptoBalance
import minerva.android.utils.BalanceUtils.getFiatBalance
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.AccountAsset
import minerva.android.walletmanager.model.Balance
import minerva.android.widget.AssetView
import minerva.android.widget.repository.getNetworkIcon
import minerva.android.wrapped.startAccountAddressWrappedActivity
import minerva.android.wrapped.startSafeAccountWrappedActivity

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

    override fun onSendAccountClicked(account: Account) = listener.onSendTransaction(account)

    override fun onSendAssetClicked(accountIndex: Int, assetIndex: Int) = listener.onSendAssetTransaction(accountIndex, assetIndex)

    override fun onAccountRemoved(position: Int) = listener.onAccountRemove(rawAccounts[position])

    override fun refreshAssets(rawPosition: Int): List<AccountAsset> = rawAccounts[rawPosition].accountAssets

    override fun onCreateSafeAccountClicked(account: Account) = listener.onCreateSafeAccount(account)

    fun updateList(data: List<Account>) {
        rawAccounts = data
        activeAccounts = data.filter { !it.isDeleted }
        notifyDataSetChanged()
    }

    fun updateBalances(balances: HashMap<String, Balance>) {
        activeAccounts.forEachIndexed { index, account ->
            account.apply {
                if (cryptoBalance != balances[address]?.cryptoBalance) {
                    cryptoBalance = balances[address]?.cryptoBalance ?: Int.InvalidId.toBigDecimal()
                    fiatBalance = balances[address]?.fiatBalance ?: Int.InvalidId.toBigDecimal()
                    notifyItemChanged(index)
                }
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
            NetworkManager.getColor(network).let { networkColor ->
                card.setCardBackgroundColor(NetworkManager.getColor(network, isPending))
                progress.apply {
                    visibleOrGone(isPending)
                    DrawableCompat.setTint(indeterminateDrawable, networkColor)
                }
                pendingMask.visibleOrGone(isPending)
                icon.setImageResource(getNetworkIcon(NetworkManager.getNetwork(network)))
                accountName.text = name
                cryptoTokenName.run {
                    text = NetworkManager.getNetwork(network).token
                    setTextColor(networkColor)
                }
                with(amountView) {
                    setCrypto(getCryptoBalance(cryptoBalance))
                    (if (NetworkManager.getNetwork(account.network).testnet) Int.InvalidValue.toBigDecimal()
                    else account.fiatBalance).let { setFiat(getFiatBalance(it)) }
                }
                sendButton.text =
                    String.format(SEND_BUTTON_FORMAT, view.context.getString(R.string.send), NetworkManager.getNetwork(network).token)
            }
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
        safeAccountBadge.gone()
    }

    private fun View.prepareSafeAccountView() {
        mainContent.run {
            margin(FRAME_WIDTH, FRAME_TOP_WIDTH, FRAME_WIDTH, FRAME_WIDTH)
            setBackgroundResource(R.drawable.safe_account_background)
        }
        safeAccountBadge.visible()
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
                R.id.showAddress -> startAccountAddressWrappedActivity(
                    view.context, account.name, position,
                    getNetworkIcon(NetworkManager.getNetwork(account.network))
                )
                R.id.safeAccountSettings -> startSafeAccountWrappedActivity(
                    view.context, account.name, position,
                    R.drawable.ic_safe_account_single_owner
                )
                R.id.addSafeAccount -> listener.onCreateSafeAccountClicked(account)
                R.id.remove -> listener.onAccountRemoved(position)
            }
            true
        }
    }

    private fun isCreatingSafeAccountAvailable(account: Account) =
        NetworkManager.isSafeAccountAvailable(account.network) && !account.isSafeAccount

    private fun isSafeAccount(account: Account) =
        NetworkManager.isSafeAccountAvailable(account.network) && account.isSafeAccount

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
        fun refreshAssets(rawPosition: Int): List<AccountAsset>
        fun onCreateSafeAccountClicked(account: Account)
    }
}