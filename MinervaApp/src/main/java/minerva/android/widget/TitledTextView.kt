package minerva.android.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.titled_text_view.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible

class TitledTextView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    fun setTitleAndBody(titleText: String, bodyText: String) {
        visible()
        title.text = titleText
        body.text = bodyText
    }

    fun setSingleLineTitleAndBody(titleText: String, bodyText: String) {
        setTitleAndBody(titleText, bodyText)
        body.apply {
            setSingleLine()
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
    }

    init {
        inflate(context, R.layout.titled_text_view, this)
        orientation = VERTICAL
        gone()
    }
}