package minerva.android.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.kotlinUtils.Space

class LetterLogo(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {

    fun createLogo(value: String) {
        text = prepareLetter(value)[FIRST_SIGN].toString().capitalize()
        setTextColor(ContextCompat.getColor(context, generateColor(value)))
        backgroundTintList = ContextCompat.getColorStateList(context, generateColor(value, true))
    }

    private fun prepareLetter(value: String): String =
        if(value.isBlank()) String.Space
        else value

    init {
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        setBackgroundResource(R.drawable.round_background)
        val sidePadding = resources.getDimensionPixelOffset(R.dimen.margin_xsmall)
        val bottomPadding = resources.getDimensionPixelOffset(R.dimen.margin_xxxsmall)
        setPadding(sidePadding, NO_PADDING, sidePadding, bottomPadding)
    }

    companion object {
        private const val NO_PADDING = 0
        private const val FIRST_SIGN = 0
    }
}