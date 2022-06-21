package minerva.android.accounts.nft.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.recyclerview.widget.RecyclerView
import minerva.android.accounts.nft.model.NftItem
import minerva.android.databinding.ItemNftBinding
import minerva.android.widget.RecyclableViewMoreTextView


class NftAdapter : RecyclerView.Adapter<NftViewHolder>() {

    private var nftList: MutableList<NftItem> = mutableListOf()
    lateinit var listener: NftViewHolder.Listener

    fun updateList(newNftList: List<NftItem>) {
        nftList = newNftList.toMutableList()
        notifyDataSetChanged()
    }

    /**
     * Update Item - method for update item which were specified
     * @param nftItem - updated instance of NftItem
     */
    fun updateItem(nftItem: NftItem) {
        //find item by id in main list; can't find by object instance because specified nftItem already changed
        val itemWhichWillBeChanged: NftItem? = nftList.find { it.tokenId == nftItem.tokenId }
        itemWhichWillBeChanged?.let { nft ->
            val position: Int = nftList.indexOf(nft)
            if (position != RecyclerView.NO_POSITION) {
                nftList[position] = nftItem
                if (position != RecyclerView.NO_POSITION)
                    notifyItemChanged(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NftViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = ItemNftBinding.inflate(inflater, parent, false)
        return NftViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: NftViewHolder, position: Int) {
        holder.bind(nftList[position])
    }

    override fun getItemCount(): Int = nftList.size
}

class NftViewHolder(val binding: ItemNftBinding, private val listener: Listener) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: NftItem) = with(binding) {
        setup(item) { nftItem ->
            setupDescription(nftItem)
        }
        btSend.setOnClickListener {
            listener.onSendClicked(item)
        }
        nftDetails.favoriteStateFlag.setOnClickListener {
            //changing "isFavorite" state to opposite value
            val nftItem: NftItem = item.copy(isFavorite = !item.isFavorite)
            listener.changeFavoriteState(nftItem)
        }
    }

    private fun ItemNftBinding.setupDescription(nftItem: NftItem) = nftDetails.description.apply {
        listener = object : RecyclableViewMoreTextView.Listener {
            override fun afterSetEllipsizedText() =
                btSend.toggleVisibility(nftItem)

            override val nextAnimation: Animation
                get() = btSend.expandAnimation(animationDurationMultiplier, maxDuration)

            override fun onExpandAnimationStarted() =
                btSend.startAnimation(nextAnimation)
        }
        setOnClickListener { expand(nftItem) }
        bind(nftItem.nftContent.description, nftItem.isDescriptionExpanded)
    }

    interface Listener {
        fun onSendClicked(nftItem: NftItem)

        /**
         * Change Favorite State - method which add selected item to favorite ntf
         * @param nftItem - instance of NftItem
         */
        fun changeFavoriteState(nftItem: NftItem)
    }
}