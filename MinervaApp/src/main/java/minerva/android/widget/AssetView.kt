package minerva.android.widget

import android.content.Context
import android.transition.TransitionManager
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.asset_layout.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.rotate180
import minerva.android.extension.rotate180back
import minerva.android.extension.visible
import java.math.BigDecimal

class AssetView(callback: AssertViewCallback, name: String, @DrawableRes logoRes: Int) : RelativeLayout(callback.getContext()) {

    private val isOpen: Boolean
        get() = sendButton.isVisible

    init {
        inflate(context, R.layout.asset_layout, this)
        assetName.text = name
        assetLogo.setImageResource(logoRes)
        sendButton.text = String.format(SEND_BUTTON_FORMAT, context.getString(R.string.send), name)
        setOnClickListener {
            TransitionManager.beginDelayedTransition(callback.getViewGroup())
            if (isOpen) close() else open()
        }
        sendButton.setOnClickListener {
            callback.onSendAssetClicked()
        }
    }

    fun setAmounts(crypto: BigDecimal, currency: Float) {
        amountView.setAmounts(crypto, currency)
    }

    private fun open() {
        arrow.rotate180()
        sendButton.visible()
    }

    private fun close() {
        arrow.rotate180back()
        sendButton.gone()
    }

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
    }

    interface AssertViewCallback {
        fun onSendAssetClicked()
        fun getViewGroup(): ViewGroup
        fun getContext(): Context
    }
}