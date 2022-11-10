package minerva.android.accounts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import minerva.android.R
import minerva.android.accounts.AccountsFragment.Companion.ADD_ITEM
import minerva.android.accounts.AccountsFragment.Companion.ITEM
import minerva.android.accounts.listener.AccountsAdapterListener
import minerva.android.accounts.listener.AccountsFragmentToAdapterListener
import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.widget.state.AccountWidgetState

class AccountAdapter(
    private val listener: AccountsFragmentToAdapterListener
) : RecyclerView.Adapter<ViewHolder>(), AccountsAdapterListener {
    private val differ: AsyncListDiffer<Account> = AsyncListDiffer(this, AccountDiffCallback())
    private var fiatSymbol: String = String.Empty

    override fun getItemViewType(position: Int): Int = if (differ.currentList[position].id != ADD_ITEM) ITEM else ADD_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        ITEM -> AccountViewHolder( // create usual account
            LayoutInflater.from(parent.context).inflate(R.layout.account_list_row, parent, false),
            parent
        )
        ADD_ITEM -> AddAccountButtonViewHolder(LayoutInflater.from(parent.context) // create add account button item
            .inflate(R.layout.add_account_list_row, parent, false), listener)
        else -> error(parent.context.getString(R.string.unknown_type))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (ITEM == getItemViewType(position)) { // usual case (account item)
            with(holder as AccountViewHolder) {
                val account = differ.currentList[position]
                setupListener(this@AccountAdapter)
                setupAccountIndex(listener.indexOf(account))
                setupAccountView(account, fiatSymbol, listener.getTokens(account))
            }
        }
    }

    override fun getItemCount(): Int = differ.currentList.size

    fun updateList(accounts: List<Account>) {
        var accountsWithAddButton = accounts.toMutableList() // added "add account" button item
        //using this flag(id=-1) for specifying where "add button" item will be placed
        accountsWithAddButton.add(Account(id = ADD_ITEM))
        differ.submitList(accountsWithAddButton)
        notifyDataSetChanged()
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
    }

    override fun onSendCoinClicked(account: Account) = listener.onSendTransaction(account)

    override fun onSendTokenClicked(account: Account, tokenAddress: String, isTokenError: Boolean) {
        listener.onSendTokenTransaction(account, tokenAddress, isTokenError)
    }

    override fun onNftCollectionClicked(account: Account, tokenAddress: String, collectionName: String, isGroup: Boolean) =
        listener.onNftCollectionClicked(account, tokenAddress, collectionName, isGroup)

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

    override fun openInExplorer(account: Account) = listener.openInExplorer(account)
}

