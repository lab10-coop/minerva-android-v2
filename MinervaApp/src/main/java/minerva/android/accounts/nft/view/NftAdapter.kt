package minerva.android.accounts.nft.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.recyclerview.widget.RecyclerView
import minerva.android.R
import minerva.android.accounts.nft.model.NftItem
import minerva.android.databinding.ItemNftBinding
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.visible
import java.math.BigDecimal
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible

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
            bind(item.description, item.isDescriptionExpanded)
            setOnClickListener {
                if (!item.isDescriptionExpanded) {
                    toggle()
                    item.isDescriptionExpanded = true
                }
            }
        }
        if (item.contentUrl.isBlank()) {
            hideLoading()
            errorView.visible()
            content.invisible()
            placeholder.visible()
            placeholder.setImageResource(R.drawable.ic_placeholder_nft)
        } else {
            prepareNftContent(item.contentUrl)
        }
        if (item.shouldBalanceBeDisplayed()) {
            balance.visible()
            balance.text = binding.root.context.resources.getString(
                R.string.amount_label,
                item.balance.toPlainString()
            )
        } else {
            balance.gone()
        }
    }

    private fun NftItem.shouldBalanceBeDisplayed() = isERC1155.and(balance > BigDecimal.ONE)

    private fun prepareNftContent(contentUrl: String) = with(binding) {
        content.webViewClient = WebViewClient()
        content.loadData(HtmlGenerator.getNftContentEncodedHtmlFromUrl(contentUrl), MIME_TYPE_HTML, ENCODING)
        hideLoading()
    }

    private fun hideLoading() = with(binding.progress) {
        cancelAnimation()
        invisible()
    }

    companion object {
        private const val MIME_TYPE_HTML = "text/html"
        private const val ENCODING = "base64"
        private const val AMOUNT = "base64"
    }
}