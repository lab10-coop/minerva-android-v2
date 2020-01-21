package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.crypto_amount_layout.view.*
import minerva.android.R
import minerva.android.kotlinUtils.InvalidId
import java.math.BigInteger

class CryptoAmountView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    fun setAmounts(crypto: BigInteger, currency: Float = WRONG_CURRENCY_VALUE) {
        cryptoAmount.text = if(crypto == Int.InvalidId.toBigInteger()) WRONG_CRYPTO_STRING else crypto.toString()
        currencyAmount.text = if(currency != WRONG_CURRENCY_VALUE) String.format(CURRENCY_FORMAT, currency)
        else String.format(WRONG_CURRENCY_STRING, currency)
    }

    init {
        inflate(context, R.layout.crypto_amount_layout, this)
        orientation = VERTICAL
    }

    companion object {
        const val WRONG_CURRENCY_VALUE = -1f
        private const val CURRENCY_FORMAT = "€ %.2f"
        private const val WRONG_CURRENCY_STRING = "€ -.--"
        private const val WRONG_CRYPTO_STRING = "-.--"
    }
}