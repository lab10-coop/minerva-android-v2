package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.crypto_amount_layout.view.*
import minerva.android.R

class CryptoAmountView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    fun setAmounts(crypto: Float, currency: Float = WRONG_CURRENCY_VALUE) {
        cryptoAmount.text = crypto.toString()
        currencyAmount.text = if(currency != WRONG_CURRENCY_VALUE) String.format(CURRENCY_FORMAT, currency)
        else String.format(WRONG_CURRENCY_STRING, currency)
    }

    init {
        inflate(context, R.layout.crypto_amount_layout, this)
        orientation = VERTICAL
    }

    companion object {
        private const val CURRENCY_FORMAT = "€ %.2f"
        private const val WRONG_CURRENCY_VALUE = -1f
        private const val WRONG_CURRENCY_STRING = "€ -.--"
    }
}