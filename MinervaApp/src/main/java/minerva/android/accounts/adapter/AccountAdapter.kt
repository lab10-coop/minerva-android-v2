package minerva.android.accounts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import minerva.android.R
import minerva.android.accounts.listener.AccountsAdapterListener
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.extension.*
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.state.AccountWidgetState

class AccountAdapter(
    private val listener: AccountsFragmentToAdapterListener
) : ListAdapter<Account, AccountViewHolder>(AccountDiffCallback()), AccountsAdapterListener {
    private var fiatSymbol: String = String.Empty

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder =
        AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.account_list_row, parent, false), parent)

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
        with(holder) {
            setupListener(this@AccountAdapter)
            setupAccountIndex(listener.indexOf(account))
            setupAccountView(account, fiatSymbol, listener.getTokens(account))
        }
    }

    override fun submitList(newList: List<Account>?) {
        val list = mutableListOf<Account>().apply {
            newList?.forEach { account -> add(account.copy()) }
        }
        super.submitList(list)
    }

    fun setFiat(fiatSymbol: String) {
        this.fiatSymbol = fiatSymbol
    }

    fun updateCoinBalance(index: Int) {
        notifyItemChanged(index)
    }

    fun updateTokenBalance(index: Int) {
        notifyItemChanged(index)
    }

    fun refreshList() {
        //TODO Optimise in MNR-477 - update Accounts singly when changing fiat currency
        notifyDataSetChanged()
    }

    fun updateSessionCount(sessionPerAccount: List<DappSessionData>) {
        listener.updateSessionCount(sessionPerAccount) { index ->
            //TODO Optimise in MNR-477 - update Account only when sessions count changes
            notifyItemChanged(index)
        }
    }

    fun setPending(index: Int, chainId: Int, isPending: Boolean, areMainNetsEnabled: Boolean) {
        listener.showPendingAccount(index, chainId, areMainNetsEnabled, isPending) { position ->
            notifyItemChanged(position)
        }
    }

    fun stopPendingTransactions() {
        listener.stopPendingAccounts()
        notifyDataSetChanged()
    }

    override fun onSendCoinClicked(account: Account) = listener.onSendTransaction(account)

    override fun onSendTokenClicked(account: Account, tokenAddress: String, isTokenError: Boolean) {
        listener.onSendTokenTransaction(account, tokenAddress, isTokenError)
    }

    override fun onAccountHide(index: Int) = listener.onAccountHide(index)

    override fun onCreateSafeAccountClicked(account: Account) = listener.onCreateSafeAccount(account)

    override fun onShowAddress(account: Account) = listener.onShowAddress(account)

    override fun onShowSafeAccountSettings(account: Account, index: Int) =
        listener.onShowSafeAccountSettings(account, index)

    override fun onWalletConnect(index: Int) = listener.onWalletConnect(index)

    override fun onManageTokens(index: Int) = listener.onManageTokens(index)

    override fun onExportPrivateKey(account: Account) = listener.onExportPrivateKey(account)

    override fun updateAccountWidgetState(index: Int, accountWidgetState: AccountWidgetState) =
        listener.updateAccountWidgetState(index, accountWidgetState)

    override fun getAccountWidgetState(index: Int): AccountWidgetState = listener.getAccountWidgetState(index)

    override fun onEditName(account: Account) = listener.onEditName(account)
}

