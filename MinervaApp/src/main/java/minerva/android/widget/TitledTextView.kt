package minerva.android.widget

import android.content.Context
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.titled_text_view.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible
import minerva.android.identities.data.getIdentityDataLabel

class TitledTextView(context: Context) : LinearLayout(context) {

    fun setTitleAndBody(titleText: String, bodyText: String) {
        visible()
        title.text = titleText
        body.text = bodyText
    }

    init {
        inflate(context, R.layout.titled_text_view, this)
        orientation = VERTICAL
        gone()
    }
}