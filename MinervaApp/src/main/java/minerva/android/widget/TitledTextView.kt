package minerva.android.widget

import android.content.Context
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.titled_text_view.view.*
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.extension.visible

class TitledTextView(context: Context) : LinearLayout(context) {

    fun setTitleAndBody(title: String, body: String) {
        visible()
        this.title.text = title
        this.body.text = body
    }

    init {
        inflate(context, R.layout.titled_text_view, this)
        orientation = VERTICAL
        gone()
    }
}