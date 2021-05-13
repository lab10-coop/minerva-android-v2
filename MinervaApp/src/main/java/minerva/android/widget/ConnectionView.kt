package minerva.android.widget

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import minerva.android.R
import minerva.android.databinding.ConnectionViewLayoutBinding

class ConnectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ConnectionViewLayoutBinding =
        ConnectionViewLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    fun setConnectionIcon(icon: Int) {
        Glide.with(context)
            .load(icon)
            .into(binding.connectionIcon)
    }

    fun setIconUrl(icon: String) {
        Glide.with(context)
            .load(Uri.parse(icon))
            .error(R.drawable.ic_default_connection)
            .into(binding.connectionIcon)
    }

    fun setConnectionIconsPadding() {
        with(binding) {
            connectionIcon.setPadding(PADDING)
            logo.setPadding(PADDING)
        }
    }

    companion object {
        private const val PADDING = 70
    }
}