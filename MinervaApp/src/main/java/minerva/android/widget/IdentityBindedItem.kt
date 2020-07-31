package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.identity_binded_item.view.*
import minerva.android.R

class IdentityBindedItem @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.identity_binded_item, this)
    }

    fun setIcon(icon: Int) {
        Glide.with(context).load(icon).centerInside().into(iconImageView)
    }

    fun setDateAndName(name: String, lastUsed: String) {
        nameTextView.text = name
        dateTextView.text = lastUsed
    }
}