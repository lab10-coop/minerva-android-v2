package minerva.android.accounts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.NewAccountListRowBinding
import minerva.android.walletmanager.model.minervaprimitives.account.Account

class NewAccountAdapter : RecyclerView.Adapter<NewAccountViewHolder>() {

    private var selectedPosition: Int = FIRST_POSITION

    private var accounts: List<Account> = emptyList()

    fun isEmpty() = accounts.isEmpty()

    fun updateList(newAccounts: List<Account>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = accounts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewAccountViewHolder =
        NewAccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.new_account_list_row, parent, false))

    override fun onBindViewHolder(holder: NewAccountViewHolder, position: Int) {
        holder.bind(accounts[position], selectedPosition == position) {
            notifyItemChanged(selectedPosition)
            selectedPosition = position
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedAccount() = accounts[selectedPosition]

    companion object {
        private const val FIRST_POSITION = 0
    }
}

class NewAccountViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val binding = NewAccountListRowBinding.bind(view)

    fun bind(account: Account, isChecked: Boolean, onClick: () -> Unit) = with(binding) {
        checkButton.isEnabled = isChecked
        setAccountData(account)
        view.setOnClickListener { onClick() }
    }

    private fun setAccountData(account: Account) = with(binding) {
        address.text = account.address
        indexValue.text = String.format(ACCOUNT_INDEX_PATTERN, account.id.inc())
    }

    companion object {
        private const val ACCOUNT_INDEX_PATTERN = "#%d"
    }
}