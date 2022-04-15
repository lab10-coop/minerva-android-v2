package minerva.android.services.dapps.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.databinding.ServicesFilterRecyclerViewNetworkItemBinding
import minerva.android.services.dapps.DappsFragment
import minerva.android.utils.MyHelper.l
import minerva.android.walletmanager.model.network.Network
import okhttp3.internal.notifyAll

class NetworksFilterAdapter(private val listener: Listener)
    : ListAdapter<Network, NetworksFilterAdapter.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ServicesFilterRecyclerViewNetworkItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false
        ), listener)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(
        private val _binding: ServicesFilterRecyclerViewNetworkItemBinding,
        private val listener: Listener
    ): RecyclerView.ViewHolder(_binding.root) {

        fun bind(network: Network) {
            with(_binding) {
                networkNameFilter.text = network.name
                itemView.setOnClickListener { view ->
                    //send selected network to parent fragment (DappsFragment)
                    listener.onNetworkSelected(network)

                    //change styles for selected item (and for previous selected)
                    if (prevSelEl == null) prevSelEl = this@ViewHolder;
                    else if (prevSelEl?.absoluteAdapterPosition != this@ViewHolder.absoluteAdapterPosition) {
                        //if selected item isn't equal to previous selected change styling of previous selected item to def style
                        prevSelEl?.itemView?.let {
                            it.setBackgroundResource(MAIN_BACKGROUND)
                            it.findViewById<TextView>(networkNameFilter.id)?.setTextColor(MAIN_TEXT_COLOR)
                        }
                        //set selected item like previous item for changing it style when item changes
                        prevSelEl = this@ViewHolder
                    }
                    //change style for selected item
                    itemView.setBackgroundResource(SELECTED_BACKGROUND)
                    networkNameFilter.setTextColor(SELECTED_TEXT_COLOR)
                }
            }
        }
    }

    interface Listener {
        /**
         * On Network Selected - method which send selected item to parent fragment
         * @param network - instance of minerva.android.walletmanager.model.network.Network
         */
        fun onNetworkSelected(network: Network)
    }

    object DIFF_CALLBACK : DiffUtil.ItemCallback<Network>() {
        override fun areItemsTheSame(oldItem: Network, newItem: Network) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Network, newItem: Network) = oldItem.chainId == newItem.chainId
    }

    companion object {
        val MAIN_BACKGROUND = R.drawable.services_filter_network_main_background
        val SELECTED_BACKGROUND = R.drawable.services_filter_network_selected_background
        val MAIN_TEXT_COLOR = Color.parseColor("#6442d1")
        val SELECTED_TEXT_COLOR = Color.WHITE
        //saving previous selected itemView for change bg style when selected itemView changes
        var prevSelEl: ViewHolder? = null
    }
}