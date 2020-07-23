package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.action_button.view.*
import minerva.android.R
import minerva.android.kotlinUtils.InvalidValue

class ActionButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = Int.InvalidValue) :
    LinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.action_button, this, true)
        val padding = resources.getDimension(R.dimen.margin_small).toInt()
        setBackgroundResource(R.drawable.rounded_white_button)
        setPadding(padding, padding, padding, padding)
        gravity = Gravity.CENTER
        orientation = HORIZONTAL
    }

    fun setIcon(drawable: Int) {
        icon.setImageResource(drawable)
    }

    fun setLabel(content: String) {
        label.text = content
    }
}