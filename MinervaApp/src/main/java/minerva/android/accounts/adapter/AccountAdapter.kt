package minerva.android.accounts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.accounts.listener.AccountsAdapterListener
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.AccountAsset
import minerva.android.walletmanager.model.Balance
import java.math.BigDecimal

class AccountAdapter(private val listener: AccountsFragmentToAdapterListener) : RecyclerView.Adapter<AccountViewHolder>(),
    AccountsAdapterListener {

    private var activeAccounts = listOf<Account>()
    private var rawAccounts = listOf<Account>()
    private var openAccounts = mutableListOf<Boolean>()

    val activeAccountsList
        get()= activeAccounts

    override fun getItemCount(): Int = activeAccounts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder =
        AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.account_list_row, parent, false), parent)

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        activeAccounts[position].let {
            val index = rawAccounts.indexOf(it)
            holder.apply {
                setData(index, it, openAccounts[position])
                setListener(this@AccountAdapter)
            }
        }
    }

    fun updateList(data: List<Account>, areMainNetsEnabled: Boolean) {
        rawAccounts = data
        activeAccounts = data.filter { !it.isDeleted }.filter { it.network.testNet == !areMainNetsEnabled }
        openAccounts = activeAccounts.map { false }.toMutableList()
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
            .forEachIndexed { index, account ->
                accountAssetBalances[account.privateKey]?.let { accountsList ->
                    account.accountAssets = accountsList
                        .filter { it.balance > NO_FUNDS }
                        .filter {
                            listener.isAssetVisible(account.address, it.asset.address)?.let { visibility ->
                                visibility
                            }.orElse {
                                listener.saveAssetVisibility(account.address, it.asset.address, true)
                                true
                            }
                        }
                    notifyItemChanged(index)
                }
            }
    }

    fun setPending(index: Int, isPending: Boolean, areMainNetsEnabled: Boolean) {
        rawAccounts.forEachIndexed { position, account ->
            if (account.id == index && account.network.testNet != areMainNetsEnabled) {
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

    override fun onSendAccountClicked(account: Account) = listener.onSendTransaction(rawAccounts.indexOf(account))

    override fun onSendAssetTokenClicked(account: Account, assetIndex: Int) {
        listener.onSendAssetTransaction(rawAccounts.indexOf(account), assetIndex)
    }

    override fun onAccountRemoved(index: Int) = listener.onAccountRemove(rawAccounts[index])

    override fun onCreateSafeAccountClicked(account: Account) = listener.onCreateSafeAccount(account)

    override fun onShowAddress(account: Account) = listener.onShowAddress(rawAccounts.indexOf(account))

    override fun onShowSafeAccountSettings(account: Account, index: Int) = listener.onShowSafeAccountSettings(account, index)

    override fun onWalletConnect(index: Int) = listener.onWalletConnect(index)

    override fun onManageAssets(index: Int) = listener.onManageAssets(index)

    override fun onOpenOrClose(index: Int, isOpen: Boolean) {
        openAccounts[index] = isOpen
    }

    companion object {
        private val NO_FUNDS = BigDecimal.valueOf(0)
    }
}

