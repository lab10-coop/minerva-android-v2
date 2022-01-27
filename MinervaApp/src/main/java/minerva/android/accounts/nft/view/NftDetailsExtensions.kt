package minerva.android.accounts.nft.view

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.webkit.WebViewClient
import com.google.android.material.button.MaterialButton
import minerva.android.R
import minerva.android.accounts.nft.model.NftItem
import minerva.android.databinding.FragmentSendNftBinding
import minerva.android.databinding.ItemNftBinding
import minerva.android.databinding.NftDetailsBinding
import minerva.android.extension.gone
import minerva.android.extension.invisible
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.widget.RecyclableViewMoreTextView
import java.math.BigDecimal


internal object Constants {
    const val MIME_TYPE_HTML = "text/html"
    const val ENCODING = "base64"
}

internal fun NftItem.shouldBalanceBeDisplayed() = isERC1155.and(balance != BigDecimal.ONE)

internal fun NftDetailsBinding.hideLoading() = with(progress) {
    cancelAnimation()
    invisible()
}

internal fun NftDetailsBinding.setupBalance(nftItem: NftItem) {
    if (nftItem.shouldBalanceBeDisplayed()) {
        balance.visible()
        balance.text = root.context.resources.getString(
            R.string.amount_label,
            nftItem.balance.toPlainString()
        )
    } else {
        balance.gone()
    }
}

internal fun NftDetailsBinding.prepareNftContent(contentUrl: String) {
    content.webViewClient = WebViewClient()
    content.loadData(
        HtmlGenerator.getNftContentEncodedHtmlFromUrl(contentUrl),
        Constants.MIME_TYPE_HTML,
        Constants.ENCODING
    )
    hideLoading()
}

internal fun NftDetailsBinding.setupContent(nftItem: NftItem) {
    if (nftItem.contentUrl.isBlank()) {
        hideLoading()
        errorView.visible()
        content.invisible()
        placeholder.visible()
        placeholder.setImageResource(R.drawable.ic_placeholder_nft)
    } else {
        prepareNftContent(nftItem.contentUrl)
    }
}

internal fun NftDetailsBinding.setupName(nftItem: NftItem) {
    name.text = nftItem.name
}


internal fun NftDetailsBinding.setup(nftItem: NftItem) {
    setupContent(nftItem)
    setupBalance(nftItem)
    setupName(nftItem)
}

internal fun ItemNftBinding.setup(
    nftItem: NftItem,
    onSetupDescription: (nftItem: NftItem) -> Unit
) {
    onSetupDescription(nftItem)
    nftDetails.setup(nftItem)
}

internal fun FragmentSendNftBinding.setup(
    nftItem: NftItem,
    onSetupDescription: (nftItem: NftItem) -> Unit
) {
    onSetupDescription(nftItem)
    nftDetails.setup(nftItem)
}


internal fun RecyclableViewMoreTextView.expand(nftItem: NftItem) {
    if (!nftItem.isDescriptionExpanded) {
        toggle()
        nftItem.isDescriptionExpanded = true
    }
}

internal fun MaterialButton.toggleVisibility(nftItem: NftItem, isAllTextVisible: Boolean) =
    visibleOrGone(nftItem.isDescriptionExpanded || isAllTextVisible)

internal fun MaterialButton.expandAnimation(
    durationMultiplier: Int,
    maxDuration: Int
): Animation {
    val matchParentMeasureSpec =
        View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY)
    val wrapContentMeasureSpec =
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight = measuredHeight

    // Older versions of android (pre API 21) cancel animations for views with a height of 0.
    layoutParams.height = 1
    invisible()
    return object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            layoutParams.height =
                if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
            if (layoutParams.height > 0) visible()
            requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }

    }.apply {
        duration = minOf(
            ((targetHeight / context.resources.displayMetrics.density) * durationMultiplier).toLong(),
            maxDuration.toLong()
        )
    }
}


