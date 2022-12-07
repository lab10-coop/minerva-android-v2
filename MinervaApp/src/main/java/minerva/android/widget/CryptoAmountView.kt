package minerva.android.widget

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.crypto_amount_layout.view.*
import minerva.android.R
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.BigInteger

class CryptoAmountView(context: Context, attributeSet: AttributeSet) :
    LinearLayout(context, attributeSet) {

    private var animator: ValueAnimator? = null
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

        var timePassedMillis = BigDecimal(System.currentTimeMillis() - startTime)
            .divide(BigDecimal(1000))
        var durationMillis = BigDecimal(86_400_000) // 1 day

        var startValue = start.add(
            speed
                .toBigDecimal()
                .divide(BigDecimal("10E17")) // to seconds
                .times(timePassedMillis.divide(BigDecimal(1000)))
            )
        var endValue: BigDecimal = startValue.add(
            speed
                .toBigDecimal()
                .divide(BigDecimal("10E17")) // to seconds
                .times(durationMillis.divide(BigDecimal(1000)))
        )

        with(ValueAnimator.ofObject(BigDecimalEvaluator(), startValue, endValue)) {
            animator = this
            duration = durationMillis.toLong()
            addUpdateListener { animation ->
                cryptoAmount.text =
                    BalanceUtils.getSuperTokenFormatBalance(animation.animatedValue as BigDecimal)

            }
            interpolator = LinearInterpolator()
            start()
        }
    }

    fun endStreamAnimation() {
        animator?.run {
            if (isStarted) {
                removeAllListeners()
                cancel()
            }
        }
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

    internal class BigDecimalEvaluator : TypeEvaluator<Any?> {
        override fun evaluate(fraction: Float, startValue: Any?, endValue: Any?): Any? {
            val start = startValue as BigDecimal?
            val end = endValue as BigDecimal?
            val result = end?.subtract(start)
            return result?.multiply(BigDecimal(fraction.toDouble()))?.add(start)
        }
    }

}
