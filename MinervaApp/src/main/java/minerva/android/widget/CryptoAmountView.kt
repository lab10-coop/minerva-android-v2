package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.crypto_amount_layout.view.*
import minerva.android.R
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class CryptoAmountView(context: Context, attributeSet: AttributeSet) :
    LinearLayout(context, attributeSet) {

    private var timer: Timer = Timer()
    private var startTime: Long = 0
    private var start: BigDecimal = BigDecimal.ZERO
    private var speed: BigInteger = BigInteger.ZERO

    init {
        inflate(context, R.layout.crypto_amount_layout, this)
        orientation = VERTICAL
    }

    fun setStreamingValues(_start: BigDecimal, _speed: BigInteger) {
        // startTime should be the block timestamp when balance was requested
        startTime = System.currentTimeMillis()
        start = _start
        speed = _speed
    }

    fun startStreamAnimation() {
        cryptoAmount.setTextColor(ContextCompat.getColor(context, R.color.bodyColor))

        /*timer.schedule(object : TimerTask() {
            var duration: BigDecimal = BigDecimal.ZERO
            var animatedValue = start

            override fun run() {
                duration = BigDecimal(System.currentTimeMillis() - startTime)
                    .divide(BigDecimal(1000))
                animatedValue = start
                    .add(
                        speed
                            .toBigDecimal()
                            .divide(BigDecimal("10E17"))
                            .times(duration)
                    )

                cryptoAmount.text =
                    BalanceUtils.getSuperTokenFormatBalance(animatedValue as BigDecimal)
            }
        }, 0, 250)*/
    }

    fun endStreamAnimation() {
        timer.cancel()
    }

    fun setCryptoBalance(cryptoBalance: String) = with(cryptoAmount) {
        text = cryptoBalance
        setTextColor(ContextCompat.getColor(context, R.color.bodyColor))
    }

    fun setErrorColor() {
        cryptoAmount.setTextColor(ContextCompat.getColor(context, R.color.titleColor))
    }

    fun setFiat(fiatBalance: String) {
        currencyAmount.text = fiatBalance
    }
}
