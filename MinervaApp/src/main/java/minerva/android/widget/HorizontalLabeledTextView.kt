package minerva.android.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.LinearLayout
import minerva.android.R
import minerva.android.databinding.HorizontalLabeledTextLayoutBinding
import minerva.android.extension.visible
import minerva.android.extension.visibleOrGone
import minerva.android.kotlinUtils.Empty

class HorizontalLabeledTextView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private var binding = HorizontalLabeledTextLayoutBinding.bind(inflate(context, R.layout.horizontal_labeled_text_layout, this))

    fun setTitleAndBody(titleText: String, bodyText: String) {
        visible()
        binding.apply {
            title.text = "$titleText:"
            body.text = bodyText
        }
    }

    fun setEllipsize(truncateAt: TextUtils.TruncateAt) {
        binding.body.apply {
            setSingleLine()
            ellipsize = truncateAt
        }
    }

    fun setDataOrHide(titleText: String, bodyText: String) {
        setTitleAndBody(titleText, bodyText)
        visibleOrGone(bodyText != String.Empty)
    }
}