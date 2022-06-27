package minerva.android.accounts.nft.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
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
     * Update Item - method for update/remove specified item
     * @param nftItem - instance of minerva.android.accounts.nft.model.NftItem
     * @param isGroup Boolean - delete item from group (for favorite group case)
     */
    fun updateItem(nftItem: NftItem, isGroup: Boolean) {
        //find item by id in main list; can't find by object instance because specified nftItem already changed
        val itemWhichWillBeChanged: NftItem? = nftList.find { it.tokenId == nftItem.tokenId }
        itemWhichWillBeChanged?.let { nft ->
            val position: Int = nftList.indexOf(nft)
            if (position != RecyclerView.NO_POSITION) {
                nftList[position] = nftItem
                //delete item from group (like favorite group)
                if (isGroup) {
                    if (position != RecyclerView.NO_POSITION) {
                        nftList.removeAt(position)
                        notifyItemRemoved(position)
                    }
                }
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

@SuppressLint("CheckResult")
class NftViewHolder(val binding: ItemNftBinding, private val listener: Listener) : RecyclerView.ViewHolder(binding.root) {
    //identity for item
    private var _id: String? = null
    //calculate and caches favorite state(changed state from first model\data(item: NftItem) which got from server)
    private var localFavoriteState: Boolean = false

    init {
        //update favorite state flag after got answer from ViewModel (after data changed)
        NftCollectionFragment.nftUpdateObservable.subscribe { updatedItem ->
            if (_id != null && _id == updatedItem.tokenId) {
                binding.nftDetails.apply {
                    //change state to opposite because we work with old(not updated) data that's why need calculate it our self
                    localFavoriteState = !localFavoriteState
                    //set favorite state image
                    if (localFavoriteState)
                        favoriteStateFlag.setImageResource(R.drawable.ic_favorite_state_chosen_flag)
                    else
                        favoriteStateFlag.setImageResource(R.drawable.ic_favorite_state_flag)
                    //set element clickable after new state was set
                    favoriteStateFlag.isClickable = true
                }
            }
        }
    }

    fun bind(item: NftItem) = with(binding) {
        //set id for item for prevent Observer changed on another items
        _id = item.tokenId
        localFavoriteState = item.isFavorite
        setup(item) { nftItem ->
            setupDescription(nftItem)
        }
        btSend.setOnClickListener {
            listener.onSendClicked(item)
        }
        nftDetails.favoriteStateFlag.setOnClickListener {
            //set element unclickable for prevent multiple click while data send/update/get
            it.isClickable = false
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