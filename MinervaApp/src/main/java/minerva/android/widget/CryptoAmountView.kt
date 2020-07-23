package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.crypto_amount_layout.view.*
import minerva.android.R

class CryptoAmountView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    init {
        inflate(context, R.layout.crypto_amount_layout, this)
        orientation = VERTICAL
    }

    fun setCrypto(crypto: String) {
        cryptoAmount.text = crypto
    }

    fun setFiat(currency: String) {
        currencyAmount.text = currency
    }
}