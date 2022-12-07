package minerva.android.widget.blockies

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import minerva.android.R
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.NativeToken
import minerva.android.walletmanager.model.token.Token

open class BlockiesImageView(context: Context, attributeSet: AttributeSet?) : AppCompatImageView(context, attributeSet) {

    private var blockies: Blockies? = null
    private var painter: BlockiesPainter = BlockiesPainter()

    fun initView(token: Token) {
        when (token) {
            is NativeToken -> prepareNativeTokenIcon(token)
            is ERCToken -> prepareERCTokenIcon(token)
        }
    }

    fun clear() {
        blockies = null
        setImageResource(R.drawable.ic_default_token)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        blockies?.let { drawBlockies(canvas, it) }.orElse {
            canvas.drawColor(Color.TRANSPARENT)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        painter.setDimensions(measuredWidth.toFloat(), measuredHeight.toFloat())
    }

    private fun prepareNativeTokenIcon(token: NativeToken) {
        blockies = null
        setImageResource(token.logoRes)
        invalidate()
    }

    private fun prepareERCTokenIcon(token: ERCToken) {
        if (token.type.isNft()) {
            this.setImageResource(R.drawable.ic_collectible_square)//set default logo for nft
            if (!token.logoURI.isNullOrEmpty()) {//set image from url
                //create coil image loader request with rounded corners for icon
                val request = ImageRequest.Builder(this.context)
                    .transformations(RoundedCornersTransformation(10f))
                    .placeholder(R.drawable.ic_collectible_square)
                    .error(R.drawable.ic_collectible_square)
                    .data(token.logoURI)
                    .target(this)
                    .build()

                context.imageLoader.enqueue(request)//set coil image loader request to coil image loader
            }
        } else {
            token.logoURI?.let { uri ->
                val icon = when {
                    uri.isEmpty() && token.symbol == USDT_SYMBOL -> {
                        R.drawable.ic_coin_usdt
                    }
                    uri.isEmpty() && token.symbol == DAI_SYMBOL -> {
                        R.drawable.ic_coin_dai
                    }
                    else -> uri
                }
                Glide.with(this).load(icon).apply(RequestOptions.circleCropTransform()).into(this)
                return
            }
            blockies = Blockies.fromAddress(token.address)
        }
        invalidate()
    }

    private fun drawBlockies(canvas: Canvas, blockies: Blockies) {
        painter.draw(canvas, blockies)
    }

    companion object {
        private const val USDT_SYMBOL = "USDT"
        private const val DAI_SYMBOL = "DAI"
    }
}