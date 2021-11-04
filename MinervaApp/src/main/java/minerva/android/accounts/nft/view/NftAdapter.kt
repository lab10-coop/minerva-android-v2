package minerva.android.accounts.nft.view

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import minerva.android.R
import minerva.android.accounts.nft.model.NftItem
import minerva.android.databinding.ItemNftBinding
import minerva.android.extension.invisible
import minerva.android.extension.visible

class NftAdapter : RecyclerView.Adapter<NftViewHolder>() {

    private var nftList: List<NftItem> = emptyList()

    fun updateList(newNftList: List<NftItem>) {
        nftList = newNftList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NftViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = ItemNftBinding.inflate(inflater, parent, false)
        return NftViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NftViewHolder, position: Int) {
        holder.bind(nftList[position])
    }

    override fun getItemCount(): Int = nftList.size
}

class NftViewHolder(val binding: ItemNftBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: NftItem) = with(binding) {
        name.text = item.name
        description.apply {
            text = item.description
            setOnClickListener { toggle() }
        }
        if (item.contentUrl.isBlank()) {
            hideLoading()
            errorView.visible()
            content.setImageResource(R.drawable.ic_placeholder_nft)
        } else {
            prepareGlideContent(item.contentUrl)
        }
    }

    private fun prepareGlideContent(contentUrl: String) = with(binding) {
        Glide.with(root)
            .load(contentUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean = false

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    hideLoading()
                    return false
                }
            }).centerCrop().into(content)
    }

    private fun hideLoading() = with(binding.progress) {
        cancelAnimation()
        invisible()
    }
}