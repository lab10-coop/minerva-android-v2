package minerva.android.accounts.nft.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.recyclerview.widget.RecyclerView
import minerva.android.accounts.nft.model.NftItem
import minerva.android.databinding.ItemNftBinding
import minerva.android.widget.RecyclableViewMoreTextView


class NftAdapter : RecyclerView.Adapter<NftViewHolder>() {

    private var nftList: List<NftItem> = emptyList()
    lateinit var listener: NftViewHolder.Listener

    fun updateList(newNftList: List<NftItem>) {
        nftList = newNftList
        notifyDataSetChanged()
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
    }

    private fun ItemNftBinding.setupDescription(nftItem: NftItem) = nftDetails.description.apply {
        listener = object : RecyclableViewMoreTextView.Listener {
            override fun afterSetEllipsizedText() =
                btSend.toggleVisibility(nftItem, isAllTextVisible())

            override val nextAnimation: Animation
                get() = btSend.expandAnimation(animationDurationMultiplier, maxDuration)

            override fun onExpandAnimationStarted() =
                btSend.startAnimation(nextAnimation)
        }
        setOnClickListener { expand(nftItem) }
        bind(nftItem.description, nftItem.isDescriptionExpanded)
    }

    interface Listener {
        fun onSendClicked(nftItem: NftItem)
    }
}