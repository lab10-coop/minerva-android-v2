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
import minerva.android.walletmanager.model.Account
import minerva.android.utils.BalanceUtils.getCryptoBalance
import minerva.android.utils.BalanceUtils.getFiatBalance
import java.math.BigDecimal

class AssetView(callback: AssertViewCallback, account: Account, assetIndex: Int, @DrawableRes logoRes: Int) :
    RelativeLayout(callback.getContext()) {

    private val isOpen: Boolean
        get() = sendButton.isVisible

    init {
        inflate(context, R.layout.asset_layout, this)
        prepareView(account, assetIndex, logoRes)
        prepareListeners(callback, account, assetIndex)
    }

    fun setAmounts(crypto: BigDecimal, fiat: BigDecimal = WRONG_CURRENCY_VALUE) {
        with(amountView) {
            setCrypto(getCryptoBalance(crypto))
            setFiat(getFiatBalance(fiat))
        }
    }

    private fun open() {
        arrow.rotate180()
        sendButton.visible()
    }

    private fun close() {
        arrow.rotate180back()
        sendButton.gone()
    }

    private fun prepareView(account: Account, assetIndex: Int, @DrawableRes logoRes: Int) {
        assetLogo.setImageResource(logoRes)
        account.assets[assetIndex].let {
            assetName.text = it.name
            sendButton.text = String.format(SEND_BUTTON_FORMAT, context.getString(R.string.send), it.nameShort)
        }
    }

    private fun prepareListeners(callback: AssertViewCallback, account: Account, assetIndex: Int) {
        setOnClickListener {
            TransitionManager.beginDelayedTransition(callback.getViewGroup())
            if (isOpen) close() else open()
        }
        sendButton.setOnClickListener { callback.onSendAssetClicked(account.index, assetIndex) }
    }

    companion object {
        private const val SEND_BUTTON_FORMAT = "%s %s"
        private val WRONG_CURRENCY_VALUE = (-1).toBigDecimal()
    }

    interface AssertViewCallback {
        fun onSendAssetClicked(accountIndex: Int, assetIndex: Int)
        fun getViewGroup(): ViewGroup
        fun getContext(): Context
    }
}