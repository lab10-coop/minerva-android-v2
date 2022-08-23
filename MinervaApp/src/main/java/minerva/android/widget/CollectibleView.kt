package minerva.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import minerva.android.R
import minerva.android.databinding.CollectibleViewBinding
import minerva.android.extension.toJsonArray
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERCToken
import java.math.BigDecimal

@SuppressLint("ViewConstructor")
class CollectibleView(context: Context) : ConstraintLayout(context) {

    private var binding = CollectibleViewBinding.bind(inflate(context, R.layout.collectible_view, this))

    fun initView(
        account: Account,
        callback: CollectibleViewCallback,
        collectible: ERCToken,
        balance: BigDecimal,
        isGroup: Boolean = false //using when creating group case(with items)(like favorite) instead of item
    ) {
        prepareView(collectible, balance)
        prepareListener(callback, account, collectible, isGroup)
    }

    private fun prepareView(collectible: ERCToken, balance: BigDecimal) = binding.apply {
        collectibleName.text = collectible.symbol
        collectibleDesc.text = collectible.collectionName
        collectibleItem.text = balance.toEngineeringString()
        collectible.logoURI?.let { logoUri -> collectibleLogo.loadUrl(logoUri) }
        if (TokensAndCollectiblesView.FAVORITE_GROUP_ID == collectible.chainId) {
            collectibleViewItemSeparator.visibility = View.VISIBLE
            collectibleDesc.visibility = View.GONE
        } else {
            collectibleViewItemSeparator.visibility = View.GONE
            collectibleDesc.visibility = View.VISIBLE
        }
    }

    private fun prepareListener(callback: CollectibleViewCallback, account: Account, collectible: ERCToken, isGroup: Boolean = false) {
        setOnClickListener {
            //packing address to json array - for transfer it like string
            val wrappedAddress: String = if (isGroup) collectible.address else collectible.address.toJsonArray()
            callback.onCollectibleClicked(
                account,
                wrappedAddress,
                collectible.collectionName ?: String.Empty,
                isGroup)
        }
    }

    interface CollectibleViewCallback {
        fun onCollectibleClicked(account: Account, tokenAddress: String, collectionName: String, isGroup: Boolean = false)
    }

    private fun ImageView.loadUrl(url: String) {
        val roundedCornerRadius = 10f
        val imageLoader = context.imageLoader
        val request = ImageRequest.Builder(this.context)
            .transformations(RoundedCornersTransformation(roundedCornerRadius))
            .placeholder(R.drawable.ic_collectible_square)
            .error(R.drawable.ic_collectible_square)
            .data(url)
            .target(this)
            .build()

        imageLoader.enqueue(request)
    }
}