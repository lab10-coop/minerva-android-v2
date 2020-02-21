package minerva.android.walletActions.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.wallet_action_list_row.view.*
import minerva.android.R
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.model.WalletActionClustered
import minerva.android.walletmanager.utils.DateUtils.getDateFromTimestamp
import minerva.android.walletmanager.utils.DateUtils.isTheDayAfterTomorrow
import minerva.android.walletmanager.utils.DateUtils.isTheSameDay
import minerva.android.walletmanager.utils.DateUtils.timestamp
import minerva.android.widget.WalletActionView

class WalletActionsAdapter : RecyclerView.Adapter<WalletActionsViewHolder>() {

    private var walletActions = listOf<WalletActionClustered>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletActionsViewHolder =
        WalletActionsViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.wallet_action_list_row, parent,
                false
            )
        )

    override fun getItemCount(): Int = walletActions.size

    override fun onBindViewHolder(holder: WalletActionsViewHolder, position: Int) {
        holder.bindData(walletActions[position])
    }

    fun updateList(walletActions: List<WalletActionClustered>) {
        this.walletActions = walletActions
        notifyDataSetChanged()
    }
}

class WalletActionsViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun bindData(walletAction: WalletActionClustered) {
        view.run {
            lastUsed.text = showDay(walletAction.lastUsed)
            addActions(walletAction.walletActions)
        }
    }

    private fun View.addActions(actions: List<WalletAction>) {
        walletActivities.removeAllViews()
        actions.forEach {
            walletActivities.addView(WalletActionView(context).apply {
                setActionStatus(it)
                setActionType(it)
            })
        }
    }

    private fun View.showDay(lastUsed: Long): CharSequence? {
        return when {
            isTheSameDay(timestamp, lastUsed) -> context.getString(R.string.today)
            isTheDayAfterTomorrow(lastUsed) -> context.getString(R.string.yesterday)
            else -> getDateFromTimestamp(lastUsed)
        }
    }
}