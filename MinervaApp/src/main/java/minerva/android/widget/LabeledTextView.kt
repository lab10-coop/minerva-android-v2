package minerva.android.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.labeled_text_view.view.*
import minerva.android.R
import minerva.android.extension.visibleOrInvisible

class LabeledTextView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    fun setTitleAndBody(titleText: String, bodyText: String) {
        title.apply {
            visibleOrInvisible(titleText.isNotBlank())
            text = titleText
        }
        body.text = bodyText
    }

    fun setSingleLine() {
        body.apply {
            setSingleLine()
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
    }

    fun makeEnabled(enabled: Boolean) {
        body.setTextColor(ContextCompat.getColor(context, getEnabledTextColor(enabled)))
        container.setBackgroundResource(getEnabledResource(enabled))
    }

    private fun getEnabledTextColor(enabled: Boolean): Int = if (enabled) R.color.bodyColor else R.color.titleColor

    private fun getEnabledResource(enabled: Boolean): Int = if (enabled) R.drawable.rounded_white_frame else R.drawable.rounded_gray_frame

    init {
        inflate(context, R.layout.labeled_text_view, this)
    }
}