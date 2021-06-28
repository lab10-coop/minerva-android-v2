package minerva.android.main.walletconnect.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.TransactionSpeedItemBinding
import minerva.android.walletmanager.model.defs.TxType
import minerva.android.walletmanager.model.transactions.TxSpeed

class TransactionSpeedAdapter : RecyclerView.Adapter<TransactionSpeedViewHolder>() {

    private var speeds: List<TxSpeed> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionSpeedViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return TransactionSpeedViewHolder(TransactionSpeedItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: TransactionSpeedViewHolder, position: Int) {
        holder.bindView(speeds[position])
    }

    override fun getItemCount(): Int = speeds.size

    fun updateSpeeds(speeds: List<TxSpeed>) {
        this.speeds = speeds
        notifyDataSetChanged()
    }

    fun getCurrentTxSpeed(position: Int): TxSpeed = speeds[position]

    fun getPositionOfTxType(txType: TxType): Int = speeds.indexOfFirst { txSpeed -> txSpeed.type == txType }
}

class TransactionSpeedViewHolder(private val binding: TransactionSpeedItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindView(txSpeed: TxSpeed) = with(binding) {
        val resId: Int = when (txSpeed.type) {
            TxType.RAPID -> R.string.rapid
            TxType.FAST -> R.string.fast
            TxType.STANDARD -> R.string.standard
            TxType.SLOW -> R.string.slow
            else -> R.string.standard
        }
        speed.text = root.context.getString(resId)
        transactionTime.text = txSpeed.label
    }
}