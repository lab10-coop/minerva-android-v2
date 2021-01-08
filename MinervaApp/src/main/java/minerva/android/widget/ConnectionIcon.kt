package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import minerva.android.databinding.ConnectionIconBinding

class ConnectionIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ConnectionIconBinding =
        ConnectionIconBinding.inflate(LayoutInflater.from(context), this, true)

    fun setIcon(resId: Int) {
        Glide.with(context)
            .load(resId)
            .into(binding.icon)
    }
}