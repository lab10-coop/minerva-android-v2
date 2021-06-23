package minerva.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import minerva.android.R
import minerva.android.databinding.ViewPredefinedNetworkRowBinding
import minerva.android.kotlinUtils.EmptyResource

class PredefinedNetworkView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var binding =
        ViewPredefinedNetworkRowBinding.bind(inflate(context, R.layout.view_predefined_network_row, this))

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.PredefinedNetworkView, Int.EmptyResource, Int.EmptyResource).run {
            val nameRes = getResourceId(R.styleable.PredefinedNetworkView_name, Int.EmptyResource)
            val iconRes = getResourceId(R.styleable.PredefinedNetworkView_icon, Int.EmptyResource)
            updateTitle(nameRes, iconRes)
        }
    }

    private fun updateTitle(nameRes: Int, iconRes: Int) = with(binding.row) {
        setText(nameRes)
        setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, Int.EmptyResource, Int.EmptyResource, Int.EmptyResource)
    }
}