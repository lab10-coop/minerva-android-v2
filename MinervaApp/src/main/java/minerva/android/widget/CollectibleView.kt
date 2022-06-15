package minerva.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.google.gson.Gson
import minerva.android.R
import minerva.android.databinding.CollectibleViewBinding
import minerva.android.extension.toJsonArray
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.widget.TokensAndCollectiblesView.Companion.TOKEN_LOGO
import java.math.BigDecimal

@SuppressLint("ViewConstructor")
class CollectibleView(context: Context) : ConstraintLayout(context) {

    private var binding = CollectibleViewBinding.bind(inflate(context, R.layout.collectible_view, this))

    fun initView(
        account: Account,
        callback: CollectibleViewCallback,
        collectible: ERCToken,
        balance: BigDecimal,
        tokenLogo: TOKEN_LOGO = TOKEN_LOGO.URI //using when creating group (with items) instead of item
    ) {
        prepareView(collectible, balance, tokenLogo)
        //use this flag when need set group item (like "favorite")
        val isGroup: Boolean = if (TOKEN_LOGO.URI != tokenLogo) true else false
        prepareListener(callback, account, collectible, isGroup)
    }

    private fun prepareView(
            collectible: ERCToken,
            balance: BigDecimal,
            tokenLogo: TOKEN_LOGO = TOKEN_LOGO.URI) = binding.apply {
        collectibleName.text = collectible.symbol
        collectibleDesc.text = collectible.collectionName
        collectibleItem.text = balance.toEngineeringString()
        //if is group case - paste the logo of specified group
        when (tokenLogo) {
            TOKEN_LOGO.FAVORITE_GROUP ->
                collectibleLogo.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_star_logo))
            TOKEN_LOGO.URI ->
                collectible.logoURI?.let { logoUri -> collectibleLogo.loadUrl(logoUri) }
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