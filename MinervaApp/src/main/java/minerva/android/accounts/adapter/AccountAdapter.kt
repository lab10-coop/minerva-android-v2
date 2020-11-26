package minerva.android.accounts.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.account_list_row.view.*
import minerva.android.R
import minerva.android.accounts.listener.AccountsAdapterListener
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
    AccountsAdapterListener {

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
        activeAccounts.filter { !it.isPending }.forEachIndexed { index, account ->
            account.apply {
                cryptoBalance = balances[address]?.cryptoBalance ?: Int.InvalidId.toBigDecimal()
                fiatBalance = balances[address]?.fiatBalance ?: Int.InvalidId.toBigDecimal()
                notifyItemChanged(index)
            }
        }
    }

    fun updateAssetBalances(accountAssetBalances: Map<String, List<AccountAsset>>) {
        activeAccounts.filter { !it.isPending }
            .forEach { account -> accountAssetBalances[account.privateKey]?.let { account.accountAssets = it } }
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

    override fun onWalletConnect() {
        listener.onWalletConnect()
    }
}

