package minerva.android.services.dapps.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.databinding.ItemTitleBinding

class TitleAdapter(private val titleRes: Int) : RecyclerView.Adapter<TitleAdapter.TitleViewHolder>() {

    var isVisible: Boolean = false

    override fun getItemCount(): Int = if (isVisible) 1 else 0

    fun setVisibility(isVisible: Boolean) {
        this.isVisible = isVisible
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = ItemTitleBinding.inflate(inflater, parent, false)
        return TitleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TitleViewHolder, position: Int) =
        holder.bind(titleRes)

    class TitleViewHolder(private val binding: ItemTitleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(titleRes: Int) = with(binding) {
            label.setText(titleRes)
        }
    }
}


