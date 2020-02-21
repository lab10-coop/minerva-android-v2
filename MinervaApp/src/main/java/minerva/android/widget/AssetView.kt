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
import minerva.android.walletmanager.model.Value
import java.math.BigDecimal

class AssetView(callback: AssertViewCallback, value: Value, assetIndex: Int, @DrawableRes logoRes: Int) :
    RelativeLayout(callback.getContext()) {

    private val isOpen: Boolean
        get() = sendButton.isVisible

    init {
        inflate(context, R.layout.asset_layout, this)
        prepareView(value, assetIndex, logoRes)
        prepareListeners(callback, value, assetIndex)
    }

    fun setAmounts(crypto: BigDecimal, currency: BigDecimal = WRONG_CURRENCY_VALUE) {
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

    private fun prepareView(value: Value, assetIndex: Int, @DrawableRes logoRes: Int) {
        assetLogo.setImageResource(logoRes)
        value.assets[assetIndex].name.let {
            assetName.text = it
            sendButton.text = String.format(SEND_BUTTON_FORMAT, context.getString(R.string.send), it)
        }
    }

    private fun prepareListeners(callback: AssertViewCallback, value: Value, assetIndex: Int) {
        setOnClickListener {
            TransitionManager.beginDelayedTransition(callback.getViewGroup())
            if (isOpen) close() else open()
        }
        sendButton.setOnClickListener {
            callback.onSendAssetClicked(value.index, assetIndex)
        }
    }

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
        private val WRONG_CURRENCY_VALUE = (-1).toBigDecimal()
    }

    interface AssertViewCallback {
        fun onSendAssetClicked(valueIndex: Int, assetIndex: Int)
        fun getViewGroup(): ViewGroup
        fun getContext(): Context
    }
}