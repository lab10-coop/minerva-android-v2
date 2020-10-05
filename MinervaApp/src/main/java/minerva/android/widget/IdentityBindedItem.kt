package minerva.android.widget

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import kotlinx.android.synthetic.main.identity_binded_item.view.*
import minerva.android.R
import minerva.android.extensions.loadImageUrl

class IdentityBindedItem @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    private val container: View = inflate(context, R.layout.identity_binded_item, this)

    fun setIcon(icon: Int) {
        Glide.with(context).load(icon).centerInside().into(iconImageView)
    }

    fun setIconUrl(url: String?) {
        iconImageView.loadImageUrl(url, R.drawable.ic_default_credential)
    }

    fun setDateAndName(name: String, lastUsed: String) {
        nameTextView.text = name
        dateTextView.text = lastUsed
    }

    fun setOnItemClickListener(action: () -> Unit) {
        container.setOnClickListener { action() }
    }
}