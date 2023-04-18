package minerva.android.widget.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.tabs.TabLayoutMediator
import minerva.android.R
import minerva.android.databinding.DialogSelectPredefinedAccountBinding
import minerva.android.databinding.PredefinedNetworkListItemBinding
import minerva.android.databinding.PredefinedNetworksFragmentBinding
import minerva.android.kotlinUtils.EIGHT
import minerva.android.kotlinUtils.FirstIndex
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.defs.ChainId
import minerva.android.walletmanager.model.network.Network
import minerva.android.widget.repository.getMainIcon

class SelectPredefinedAccountDialog(val dialogContext: Context, private val predefinedNetworkOnClick: (List<Int>) -> Unit) : Dialog(dialogContext, R.style.DialogStyle) {

    private val binding = DialogSelectPredefinedAccountBinding.inflate(LayoutInflater.from(dialogContext))
    val activeNetworksIdsList: MutableList<Int> = mutableListOf(ChainId.GNO)//store list of active networks(default active for GNO chain)

    init {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(false)
        initView()
    }

    private fun initView() = with(binding) {
        binding.apply {
            predefinedNetworkPager.apply {
                adapter = PredefinedNetworkPager(this@SelectPredefinedAccountDialog)
                TabLayoutMediator(predefinedNetworkPageCountIndicator, predefinedNetworkPager) { _, _ -> }.attach()//set pager count indicator
            }
        }
        predefinedNetworkListButton.setOnClickListener {
            predefinedNetworkOnClick(activeNetworksIdsList)
            dismiss()
        }
    }

    /**
     * Get Sorted Networks - get and sort networks for creating started network list
     * @return List<Network>
     */
    fun getSortedNetworks(): List<Network> {
        //get active (not testNet) networks without GNO (because GNO will be added later (instead of hard sorting) like first item of list)
        val networks: MutableList<Network> = NetworkManager.networks.filter { it.isActive && !it.testNet && ChainId.GNO != it.chainId }.toMutableList()
        networks.add(Int.FirstIndex, NetworkManager.getNetwork(ChainId.GNO))//add GNO to networks list like first item
        return networks
    }
}

/**
 * Predefined Network Pager - pager for separating list of networks
 * @param dialog - context for inner pager widgets
 */
class PredefinedNetworkPager(private val dialog: SelectPredefinedAccountDialog) : FragmentStateAdapter(dialog.dialogContext as FragmentActivity) {
    override fun getItemCount(): Int {
        val itemInContainer: Int = Int.EIGHT//max count items(network) in page
        val itemsCount: Int = Math.ceil(dialog.getSortedNetworks().size.toDouble() / itemInContainer.toDouble()).toInt()//calculate pages count
        return itemsCount
    }

    override fun createFragment(position: Int): Fragment {
        val startPosition: Int = position * Int.EIGHT//get start position for get necessary list of Network(s) for chosen pager page
        return PredefinedNetworksFragment(dialog, startPosition)
    }
}

/**
 * Predefined Networks Fragment - fragment which showing specified pager page
 * @param dialog - context which all necessary props and methods
 * @param position - position of pager page
 */
class PredefinedNetworksFragment(private val dialog: SelectPredefinedAccountDialog, private val position: Int) : Fragment(R.layout.predefined_networks_fragment) {
    val predefinedNetworkListAdapter: PredefinedNetworkListAdapter = PredefinedNetworkListAdapter(dialog)
    var binding: PredefinedNetworksFragmentBinding? = null
    private val ERROR_LIST_MESSAGE = "INCORRECT LIST SPECIFIED"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = PredefinedNetworksFragmentBinding.bind(view)
        binding?.apply {
            val manager: FlexboxLayoutManager = FlexboxLayoutManager(dialog.dialogContext).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.CENTER
                alignItems = AlignItems.CENTER
            }

            predefinedNetworksRecyclerView.apply {
                adapter = predefinedNetworkListAdapter
                predefinedNetworkListAdapter.submitList(getNetworksByPagePosition(position))
                layoutManager = manager
            }
        }
    }

    /**
     * GetNetworksByPagePosition - get range from List<Network> for pager page
     * @param from - start position of list range
     * @return List<Network> - range from List<Network> for pager page
     */
    private fun getNetworksByPagePosition(from: Int): List<Network> {
        val networks: List<Network> = dialog.getSortedNetworks()//full list for getting range
        val fromToIndex: Int = from//start position of range
        val itemCount: Int = Int.EIGHT//count items in range
        var maxRequestedIndex: Int = fromToIndex + itemCount//last position of range
        if (networks.isNotEmpty() && networks.size > from) {
            if (networks.size < maxRequestedIndex) {//if this is last pager page - there is (maybe) less items for container
                maxRequestedIndex = networks.size
            }
            return networks.subList(fromToIndex, maxRequestedIndex)//get range by specified positions
        } else {
            error(ERROR_LIST_MESSAGE)
        }
    }
}

/**
 * PredefinedNetworkListAdapter
 * @param dialog - dialog with context for adapter widget =s and callback
 */
class PredefinedNetworkListAdapter(private val dialog: SelectPredefinedAccountDialog) :
    ListAdapter<Network, PredefinedNetworkListAdapter.PredefinedNetworkListViewHolder>(PredefinedNetworkList2ViewHolderDiffCallback)
{
    inner class PredefinedNetworkListViewHolder(private val itemBinding: PredefinedNetworkListItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        init {
            itemBinding.root.setOnClickListener {
                if (RecyclerView.NO_POSITION != bindingAdapterPosition) {
                    update(bindingAdapterPosition)
                }
            }
        }

        fun bind(network: Network) {
            itemBinding.apply {
                predefinedNetworkListItemText.text = network.name
                /* for future realization of "ic_predefined_lock" functional
                if (ChainId.GNO == network.chainId) {
                    predefinedNetworkListLock.setImageDrawable(ContextCompat.getDrawable(context,  R.drawable.ic_predefined_lock))
                }
                */
                predefinedNetworkListIcon.setBackgroundResource(getMainIcon(network.chainId))
                if (dialog.activeNetworksIdsList.contains(getItem(bindingAdapterPosition).chainId)) {
                    networkSelectContainer.strokeColor = dialog.dialogContext.getColor(R.color.colorPrimary)
                } else {
                    networkSelectContainer.strokeColor = dialog.dialogContext.getColor(R.color.white)
                }
            }
        }
    }

    /**
     * Update - update state of adapter
     * @param position - last user chosen item (Network)
     */
    fun update(position: Int) {
        val chainId = getItem(position).chainId
        dialog.activeNetworksIdsList.apply {
            if (contains(chainId)) {
                remove(chainId)
            } else {
                add(chainId)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PredefinedNetworkListViewHolder = PredefinedNetworkListViewHolder(
            PredefinedNetworkListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: PredefinedNetworkListViewHolder, position: Int) = holder.bind(getItem(position))

    object PredefinedNetworkList2ViewHolderDiffCallback: DiffUtil.ItemCallback<Network>() {
        override fun areItemsTheSame(oldItem: Network, newItem: Network) = oldItem.chainId == newItem.chainId
        override fun areContentsTheSame(oldItem: Network, newItem: Network) =  oldItem == newItem
    }
}