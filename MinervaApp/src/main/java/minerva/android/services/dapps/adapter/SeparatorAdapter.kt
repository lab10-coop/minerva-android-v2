package minerva.android.services.dapps.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import minerva.android.databinding.ItemSeparatorBinding

class SeparatorAdapter : RecyclerView.Adapter<SeparatorAdapter.SeparatorViewHolder>() {

    var isVisible: Boolean = false

    override fun getItemCount(): Int = if (isVisible) 1 else 0

    fun setVisibility(isVisible: Boolean) {
        this.isVisible = isVisible
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeparatorViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = ItemSeparatorBinding.inflate(inflater, parent, false)
        return SeparatorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SeparatorViewHolder, position: Int) {}

    class SeparatorViewHolder(binding: ItemSeparatorBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }
}