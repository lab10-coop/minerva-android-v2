package minerva.android.accounts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.accounts.listener.AccountsAdapterListener
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.extension.*
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.CoinBalance
import minerva.android.widget.state.AccountWidgetState

class AccountAdapter(
    private val listener: AccountsFragmentToAdapterListener
) : RecyclerView.Adapter<AccountViewHolder>(), AccountsAdapterListener {

    private var activeAccounts = listOf<Account>()
    private var rawAccounts = listOf<Account>()
    private var fiatSymbol: String = String.Empty

    override fun getItemCount(): Int = activeAccounts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder =
        AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.account_list_row, parent, false), parent)

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = activeAccounts[position]
        with(holder) {
            setupListener(this@AccountAdapter)
            setupAccountIndex(rawAccounts.indexOf(account))
            setupAccountView(account, fiatSymbol, listener.getTokens(account))
        }
    }

    fun setAccounts(accounts: List<Account>, activeAccounts: List<Account>, fiatSymbol: String) {
        rawAccounts = accounts
        this.activeAccounts = activeAccounts
        this.fiatSymbol = fiatSymbol
        notifyDataSetChanged()
    }

    fun updateBalances(balances: List<CoinBalance>) {
        activeAccounts.filter { !it.isPending }.forEachIndexed { index, account ->
            account.apply {
                val balanceData = balances.find { balance -> balance.chainId == account.chainId && balance.address.equals(account.address, true) }
                cryptoBalance = balanceData?.balance?.cryptoBalance ?: Double.InvalidValue.toBigDecimal()
                fiatBalance = balanceData?.balance?.fiatBalance ?: Double.InvalidValue.toBigDecimal()
                notifyItemChanged(index)
            }
        }
    }

    fun updateTokenBalances() {
        notifyDataSetChanged()
    }


    fun updateSessionCount(dappSessionDataList: List<DappSessionData>) {
        activeAccounts.filter { !it.isPending }.forEachIndexed { index, account ->
            account.apply {
                dappSessionCount = dappSessionDataList.find { data -> data.address == address && data.chainId == chainId }?.count ?: NO_DAPP_SESSION
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
        rawAccounts.forEach { account -> account.isPending = false }
        notifyDataSetChanged()
    }

    override fun onSendAccountClicked(account: Account) =
        listener.onSendTransaction(rawAccounts.indexOf(account))

    override fun onSendTokenClicked(account: Account, tokenAddress: String) {
        listener.onSendTokenTransaction(rawAccounts.indexOf(account), tokenAddress)
    }

    override fun onAccountHide(index: Int) = listener.onAccountHide(rawAccounts[index])

    override fun onCreateSafeAccountClicked(account: Account) = listener.onCreateSafeAccount(account)

    override fun onShowAddress(account: Account) = listener.onShowAddress(rawAccounts.indexOf(account))

    override fun onShowSafeAccountSettings(account: Account, index: Int) =
        listener.onShowSafeAccountSettings(account, index)

    override fun onWalletConnect(index: Int) = listener.onWalletConnect(index)

    override fun onManageTokens(index: Int) = listener.onManageTokens(index)

    override fun onExportPrivateKey(account: Account) = listener.onExportPrivateKey(account)

    override fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState) =
        listener.updateAccountWidgetState(index, accountWidgetState)

    override fun getAccountWidgetState(index: Int): AccountWidgetState = listener.getAccountWidgetState(index)

    override fun onEditName(account: Account) = listener.onEditName(account)

    companion object {
        private const val NO_DAPP_SESSION = 0
    }
}

