package minerva.android.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.labeled_text_view.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.extension.visibleOrInvisible

class LabeledTextView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    fun setTitleAndBody(titleText: String, bodyText: String) {
        visible()
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

    init {
        inflate(context, R.layout.labeled_text_view, this)
        gone()
    }
}