package minerva.android.accounts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.network_list_row.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.widget.repository.getNetworkIcon

class NetworkAdapter : RecyclerView.Adapter<NetworkViewHolder>() {

    private var networks: List<Network> = NetworkManager.networks
    private var selectedPosition: Int = 0

    override fun getItemCount(): Int = networks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkViewHolder =
        NetworkViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.network_list_row, parent, false))

    override fun onBindViewHolder(holder: NetworkViewHolder, position: Int) {
        holder.setData(networks[position], selectedPosition == position)
        holder.getView().setOnClickListener {
            notifyItemChanged(selectedPosition)
            selectedPosition = position
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedNetwork() = networks[selectedPosition]
}

class NetworkViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun getView() = view

    fun setData(network: Network, isChecked: Boolean) {
        view.apply {
            networkIcon.setImageResource(getNetworkIcon(network))
            checkButton.isEnabled = isChecked
            setNetworkName(network)
        }
    }

    private fun View.setNetworkName(network: Network) {
        networkName.text = if (network.https.isEmpty())
            setDisabledNetwork(network) else network.full
    }

    private fun View.setDisabledNetwork(network: Network): String {
        isEnabled = false
        checkButton.gone()
        networkName.setTextColor(context.getColor(R.color.titleColor))
        return "${network.full} - ${context.getString(R.string.soon)}"
    }
}