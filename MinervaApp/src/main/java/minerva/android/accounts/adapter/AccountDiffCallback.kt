package minerva.android.accounts.adapter

import androidx.recyclerview.widget.DiffUtil
import minerva.android.walletmanager.model.minervaprimitives.account.Account

class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
    override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean = oldItem == newItem
}
