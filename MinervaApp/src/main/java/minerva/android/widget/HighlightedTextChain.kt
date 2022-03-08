package minerva.android.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import kotlinx.android.synthetic.main.highlighted_text_chain_layout.view.*
import minerva.android.R
import minerva.android.kotlinUtils.Empty

class HighlightedTextChain(context: Context, attrs: AttributeSet? = null) : FlexboxLayout(context, attrs) {

    fun prepareChain(chainData: List<String>, title: String = String.Empty, defaultData: String? = null) {
        header.text = title
        if (chainData.isEmpty() && defaultData != null) addView(prepareCell(defaultData))
        chainData.forEach {
            addView(prepareCell(it))
        }
    }

    private fun prepareCell(cellText: String): TextView =
        TextView(context).apply {
            text = cellText.capitalize()
            background = context.getDrawable(R.drawable.rounded_purple_background)
            typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.roboto_font_family), Typeface.NORMAL)
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setPadding(
                getDimen(R.dimen.margin_xsmall),
                getDimen(R.dimen.margin_xxsmall),
                getDimen(R.dimen.margin_xsmall),
                getDimen(R.dimen.margin_xxsmall)
            )
        }

    private fun getDimen(dimenRes: Int) = context.resources.getDimensionPixelSize(dimenRes)

    init {
        inflate(context, R.layout.highlighted_text_chain_layout, this)
        flexWrap = FlexWrap.WRAP
        setShowDivider(SHOW_DIVIDER_MIDDLE)
        setDividerDrawable(context.getDrawable(R.drawable.highlighted_chain_divider))
    }
}