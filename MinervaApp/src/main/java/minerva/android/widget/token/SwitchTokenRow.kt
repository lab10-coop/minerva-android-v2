package minerva.android.widget.token

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import minerva.android.R
import minerva.android.databinding.SwitchTokenRowBinding
import minerva.android.kotlinUtils.NO_PADDING
import minerva.android.walletmanager.model.token.ERCToken

class SwitchTokenRow(context: Context, attributeSet: AttributeSet? = null) : LinearLayout(context, attributeSet) {

    private var binding: SwitchTokenRowBinding = SwitchTokenRowBinding.bind(
        inflate(context, R.layout.switch_token_row, this)
    )

    fun initView(token: ERCToken, isChecked: Boolean, typeSeparatorVisibility: Boolean, onCheckChangeAction: (address: String, isChecked: Boolean) -> Unit) {
        binding.tokenSwitch.apply {
            text = token.symbol
            this.isChecked = isChecked
            gravity = Gravity.CENTER_VERTICAL
            setOnCheckedChangeListener { _, _ -> onCheckChangeAction(token.address, this.isChecked) }
        }
        binding.tokenLogo.initView(token)
        resources.getDimension(R.dimen.margin_xsmall).toInt().let { padding ->
            setPadding(Int.NO_PADDING, Int.NO_PADDING, Int.NO_PADDING, padding)
        }
        binding.typeSeparator.visibility = if (typeSeparatorVisibility) VISIBLE else GONE
    }
}