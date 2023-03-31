package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import minerva.android.R
import minerva.android.databinding.DialogSelectPredefinedAccountBinding
import minerva.android.databinding.PredefinedNetworkListItemBinding
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.network.Network
import minerva.android.widget.repository.getMainIcon

class SelectPredefinedAccountDialog(context: Context, private val predefinedNetworkOnClick: (List<Int>) -> Unit) :
    Dialog(context, R.style.DialogStyle), PredefinedNetworkListHandleClick {

    private val binding = DialogSelectPredefinedAccountBinding.inflate(LayoutInflater.from(context))
    private val predefinedNetworkListAdapter: PredefinedNetworkListAdapter = PredefinedNetworkListAdapter(context, this)

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(false)
        initView()
    }

    private fun initView() = with(binding) {
        //create and set flexible container for dynamic count of network
        val flexboxLayoutManager: FlexboxLayoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.CENTER
            alignItems = AlignItems.CENTER
        }
        //get active (not testNet) networks without GNO (because GNO will be added later (instead of hard sorting) like first item of list)
        val networks: MutableList<Network> = NetworkManager.networks.filter { it.isActive && !it.testNet && ChainId.GNO != it.chainId }.toMutableList()
        networks.add(Int.FirstIndex, NetworkManager.getNetwork(ChainId.GNO))//add GNO to networks list like first item
        binding.apply {
            predefinedNetworkListRecyclerView.apply {
                layoutManager = flexboxLayoutManager
                adapter = predefinedNetworkListAdapter
                predefinedNetworkListAdapter.submitList(networks)
            }
        }
        predefinedNetworkListButton.setOnClickListener {
            doOnSelectedNetwork(predefinedNetworkListAdapter.getActivesIds())
        }
    }

    /**
     * Set To Active State - method which toggle active state for chosen network item
     */
    override fun setToActiveState(position: Int) = predefinedNetworkListAdapter.update(position)

    private fun doOnSelectedNetwork(chainIds: List<Int>) {
        predefinedNetworkOnClick(chainIds)
        dismiss()
    }
}

/**
 * PredefinedNetworkListAdapter - adapter for showing active networks
 * @param context - main Context of app
 * @param dialog - object which call adapter (using like listener which handle event)
 */
class PredefinedNetworkListAdapter(private val context: Context, private val dialog: SelectPredefinedAccountDialog) :
    ListAdapter<Network, PredefinedNetworkListAdapter.PredefinedNetworkListViewHolder>(PredefinedNetworkListViewHolderDiffCallback)
{
    val activeNetworkList: MutableList<Int> = mutableListOf(Int.FirstIndex)//store list of active networks(default active for GNO chain)

    /**
     * Get Actives Ids - get chain ids of chosen networks
     * @return list with chosen networks ids
     */
    fun getActivesIds(): List<Int> = activeNetworkList.map { currentList[it].chainId }

    /**
     * Update - update state of adapter
     * @param position - last user chosen item (Network)
     */
    fun update(position: Int) {
        activeNetworkList.apply {
            if (contains(position)) {
                remove(position)
            } else {
                add(position)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredefinedNetworkListViewHolder =
        PredefinedNetworkListViewHolder(
            PredefinedNetworkListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PredefinedNetworkListViewHolder, position: Int) = holder.bind(getItem(position))

    object PredefinedNetworkListViewHolderDiffCallback: DiffUtil.ItemCallback<Network>() {
        override fun areItemsTheSame(oldItem: Network, newItem: Network) = oldItem.chainId == newItem.chainId
        override fun areContentsTheSame(oldItem: Network, newItem: Network) =  oldItem == newItem
    }

    /**
     * Predefined Network List View Holder - class which fill and store network item
     * @param itemBinding - empty template for network item
     */
    inner class PredefinedNetworkListViewHolder(private val itemBinding: PredefinedNetworkListItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        init {
            itemBinding.root.setOnClickListener {
                if (RecyclerView.NO_POSITION != bindingAdapterPosition) {
                    dialog.setToActiveState(bindingAdapterPosition)
                }
            }
        }

        fun bind (network: Network) {
            itemBinding.apply {
                predefinedNetworkListItemText.text = network.name
                /* for future realization of "ic_predefined_lock" functional
                if (ChainId.GNO == network.chainId) {
                    predefinedNetworkListLock.setImageDrawable(ContextCompat.getDrawable(context,  R.drawable.ic_predefined_lock))
                }
                */
                predefinedNetworkListIcon.setBackgroundResource(getMainIcon(network.chainId))
                if (activeNetworkList.contains(bindingAdapterPosition)) {
                    networkSelectContainer.strokeColor = context.getColor(R.color.colorPrimary)
                } else {
                    networkSelectContainer.strokeColor = context.getColor(R.color.white)
                }
            }
        }
    }
}

/**
 * Predefined Network List Handle Click - used for handle events between classes
 */
interface PredefinedNetworkListHandleClick {
    /**
     * Set To Active State - handle user click
     */
    fun setToActiveState(position: Int)
}