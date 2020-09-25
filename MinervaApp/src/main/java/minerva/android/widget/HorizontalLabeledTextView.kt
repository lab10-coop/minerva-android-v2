package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.horizontal_labeled_text_layout.view.*
import minerva.android.R

class HorizontalLabeledTextView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    fun setTitleAndBody(titleText: String, bodyText: String) {
        title.text = "$titleText:"
        body.text = bodyText
    }

    init {
        inflate(context, R.layout.horizontal_labeled_text_layout, this)
    }
}