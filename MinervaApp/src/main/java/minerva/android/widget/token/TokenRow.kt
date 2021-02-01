package minerva.android.widget.token

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import minerva.android.R
import minerva.android.databinding.TokenRowBinding
import minerva.android.kotlinUtils.NO_PADDING
import minerva.android.walletmanager.model.token.Token

class TokenRow(context: Context, attributeSet: AttributeSet? = null) : LinearLayout(context, attributeSet) {

    private var binding: TokenRowBinding = TokenRowBinding.bind(
        inflate(context, R.layout.token_row, this)
    )

    fun initView(token: Token) {
        binding.apply {
            tokenLogo.initView(token)
            tokenName.text = token.symbol
            resources.getDimension(R.dimen.margin_small).toInt().let { padding ->
                setPadding(Int.NO_PADDING, padding, Int.NO_PADDING, padding)
            }
        }
    }
}