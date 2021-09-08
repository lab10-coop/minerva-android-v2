package minerva.android.widget

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.crypto_amount_layout.view.*
import minerva.android.R
import minerva.android.walletmanager.utils.BalanceUtils
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class CryptoAmountView(context: Context, attributeSet: AttributeSet) :
    LinearLayout(context, attributeSet) {

    private var animator: ValueAnimator? = null

    init {
        inflate(context, R.layout.crypto_amount_layout, this)
        orientation = VERTICAL
    }

    fun startStreamingAnimation(start: BigDecimal, end: BigDecimal, speed: BigInteger) {
        var netFlow = speed
        with(ValueAnimator.ofObject(BigDecimalEvaluator(), start, end)) {
            animator = this
            if (speed.signum() == NEGATIVE) netFlow = netFlow.abs()
            duration = netFlow.toLong()
            addUpdateListener { animation ->
                cryptoAmount.text =
                    BalanceUtils.getSuperTokenFormatBalance(animation.animatedValue as BigDecimal)
            }
            interpolator = LinearInterpolator()
            executeAnimation(speed)
        }
    }

    private fun ValueAnimator.executeAnimation(speed: BigInteger) {
        if (speed.signum() == NEGATIVE) {
            reverse()
        } else {
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

    companion object {
        private const val NEGATIVE = -1
    }
}