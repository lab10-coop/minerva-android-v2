package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.crypto_amount_layout.view.*
import minerva.android.R
import minerva.android.kotlinUtils.InvalidValue

import java.math.BigDecimal

class CryptoAmountView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    fun setAmounts(crypto: BigDecimal, currency: BigDecimal = Int.InvalidValue.toBigDecimal()) {
        cryptoAmount.text = if (crypto == Int.InvalidValue.toBigDecimal()) WRONG_CRYPTO_STRING else crypto.toPlainString()
        currencyAmount.text = if (currency != Int.InvalidValue.toBigDecimal()) String.format(CURRENCY_FORMAT, currency)
        else String.format(WRONG_CURRENCY_STRING, currency)
    }

    init {
        inflate(context, R.layout.crypto_amount_layout, this)
        orientation = VERTICAL
    }

    companion object {
        private const val CURRENCY_FORMAT = "€ %.2f"
        private const val WRONG_CURRENCY_STRING = "€ -.--"
        private const val WRONG_CRYPTO_STRING = "-.--"
    }
}