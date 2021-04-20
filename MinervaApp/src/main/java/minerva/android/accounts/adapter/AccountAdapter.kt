package minerva.android.accounts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.accounts.listener.AccountsAdapterListener
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.extension.*
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.Balance
import minerva.android.widget.state.AccountWidgetState

class AccountAdapter(private val listener: AccountsFragmentToAdapterListener) :
    RecyclerView.Adapter<AccountViewHolder>(),
    AccountsAdapterListener {

    private var activeAccounts = listOf<Account>()
    private var rawAccounts = listOf<Account>()

    override fun getItemCount(): Int = activeAccounts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder = AccountViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.account_list_row, parent, false),
        parent
    )

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        activeAccounts[position].let {
            val index = rawAccounts.indexOf(it)
            holder.setData(index, it, this@AccountAdapter)
        }
    }

    fun updateList(accounts: List<Account>, activeAccounts: List<Account>) {
        rawAccounts = accounts
        this.activeAccounts = activeAccounts
        notifyDataSetChanged()
    }

    fun updateSessionCount(accounts: HashMap<String, Int>) {
        activeAccounts.filter { !it.isPending }.forEachIndexed { index, account ->
            account.apply {
                dappSessionCount = accounts[address] ?: 0
                notifyItemChanged(index)
            }
        }
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

    fun updateTokenBalances() {
        notifyDataSetChanged()
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

    override fun onSendAccountClicked(account: Account) =
        listener.onSendTransaction(rawAccounts.indexOf(account))

    override fun onSendTokenClicked(account: Account, tokenAddress: String) {
        listener.onSendTokenTransaction(rawAccounts.indexOf(account), tokenAddress)
    }

    override fun onAccountRemoved(index: Int) = listener.onAccountRemove(rawAccounts[index])

    override fun onCreateSafeAccountClicked(account: Account) = listener.onCreateSafeAccount(account)

    override fun onShowAddress(account: Account) = listener.onShowAddress(rawAccounts.indexOf(account))

    override fun onShowSafeAccountSettings(account: Account, index: Int) = listener.onShowSafeAccountSettings(account, index)

    override fun onWalletConnect(index: Int) = listener.onWalletConnect(index)

    override fun onManageTokens(index: Int) = listener.onManageTokens(index)

    override fun onExportPrivateKey(account: Account) = listener.onExportPrivateKey(account)

    override fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState) =
        listener.updateAccountWidgetState(index, accountWidgetState)

    override fun getAccountWidgetState(index: Int): AccountWidgetState = listener.getAccountWidgetState(index)
}

