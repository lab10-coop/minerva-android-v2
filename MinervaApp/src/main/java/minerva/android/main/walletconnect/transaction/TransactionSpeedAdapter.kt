package minerva.android.main.walletconnect.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.databinding.TransactionSpeedItemBinding

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
}

class TransactionSpeedViewHolder(private val binding: TransactionSpeedItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindView(txSpeed: TxSpeed) = with(binding) {
        speed.text = txSpeed.type
        "${txSpeed.value} ${txSpeed.time}".also { transactionTime.text = it }
    }
}