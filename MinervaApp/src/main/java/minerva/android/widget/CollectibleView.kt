package minerva.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import minerva.android.R
import minerva.android.databinding.CollectibleViewBinding
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
        balance: BigDecimal
    ) {
        prepareView(collectible, balance)
        prepareListener(callback, account, collectible)
    }

    private fun prepareView(collectible: ERCToken, balance: BigDecimal) = binding.apply {
        collectibleName.text = collectible.collectionName
        collectibleDesc.text = collectible.symbol
        collectibleItem.text = balance.toEngineeringString()
        collectible.logoURI?.let { logoUri -> collectibleLogo.loadUrl(logoUri) }
    }

    private fun prepareListener(callback: CollectibleViewCallback, account: Account, collectible: ERCToken) {
        setOnClickListener {
            callback.onCollectibleClicked(account, collectible.address, collectible.collectionName ?: String.Empty)
        }
    }

    interface CollectibleViewCallback {
        fun onCollectibleClicked(account: Account, tokenAddress: String, collectionName: String)
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