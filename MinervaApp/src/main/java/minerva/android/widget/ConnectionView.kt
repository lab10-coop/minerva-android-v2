package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import minerva.android.databinding.ConnectionViewLayoutBinding

class ConnectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ConnectionViewLayoutBinding =
        ConnectionViewLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    fun setConnectionIcon(resId: Int) {
        binding.connectionIcon.setIcon(resId)
    }
}