package minerva.android.services.dapps.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import minerva.android.databinding.DappListRowBinding
import minerva.android.services.dapps.model.Dapp

class DappsAdapter(private val listener: Listener) :
    ListAdapter<Dapp, DappsAdapter.ViewHolder>(Dapp.DIFF_CALLBACK) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            DappListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )

    interface Listener {
        fun onDappSelected(onDappSelected: OnDappSelected)
        data class OnDappSelected(val dapp: Dapp)
    }

    class ViewHolder(
        private val binding: DappListRowBinding,
        private val listener: Listener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dapp: Dapp) {
            with(binding) {
                dappName.text = dapp.label
                mainContent.apply {
                    background = ContextCompat.getDrawable(mainContent.context, dapp.background)
                    setOnClickListener { listener.onDappSelected(Listener.OnDappSelected(dapp)) }
                }
            }
        }
    }
}

