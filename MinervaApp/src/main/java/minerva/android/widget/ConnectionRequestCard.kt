package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.connection_request_card_layout.view.*
import minerva.android.R

class ConnectionRequestCard
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.connection_request_card_layout, this)
    }

    fun setRequestedData(data: String) {
        requested.text = data
    }

}