package minerva.android.accounts.nft.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
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
    //identity for item for prevent Observer changed on another items
    private var _id: String? = null
    //calculate favorite state
    //if localFavoriteState = null - this means view\item not initialized yet and must use last received value from ::bind
    private var localFavoriteState: Boolean? = null

    init {
        //update favorite state flag after got answer from ViewModel (after data changed)
        NftCollectionFragment.nftUpdateObservable.subscribe { updatedItem ->
            if (_id != null && _id == updatedItem.tokenId) {
                binding.nftDetails.apply {
                    changeFavoriteStateIcon(favoriteStateFlag, updatedItem.isFavorite)
                    //set element clickable after new state was set
                    favoriteStateFlag.isClickable = true
                }
            }
        }
    }

    fun bind(item: NftItem) = with(binding) {
        //set id for item for prevent Observer changed on another items
        _id = item.tokenId
        setup(item) { nftItem ->
            setupDescription(nftItem)
        }
        btSend.setOnClickListener {
            listener.onSendClicked(item)
        }
        nftDetails.favoriteStateFlag.setOnClickListener {
            //set element to unclickable for prevent multiple click while data send/update/get
            it.isClickable = false
            //if null - init it with last data we received
            if (null == localFavoriteState) {
                localFavoriteState = !item.isFavorite //change state to opposite
            } else {
                localFavoriteState = !localFavoriteState!! //change state to opposite
            }
            //set specified favorite state for item
            localFavoriteState?.let { favState ->
                changeFavoriteStateIcon(it as ImageView, favState)
                //create model with updated state
                val nftItem: NftItem = item.copy(isFavorite = favState)
                listener.changeFavoriteState(nftItem)
            }
        }
    }

    /**
     * Change Favorite Icon - set favorite state icon
     * @param view - ImageView for set favorite state
     * @param state - Boolean shows which state need to be install
     */
    private fun changeFavoriteStateIcon(view: ImageView, state: Boolean) = if (state)
            view.setImageResource(NftCollectionFragment.FAVORITE_STATE_ENABLE)
        else
            view.setImageResource(NftCollectionFragment.FAVORITE_STATE_DISABLED)

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