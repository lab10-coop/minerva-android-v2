package minerva.android.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.databinding.TokenViewLayoutBinding
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.utils.BalanceUtils.getCryptoBalance
import minerva.android.utils.BalanceUtils.getFiatBalance
import minerva.android.walletmanager.model.Account
import java.math.BigDecimal

class TokenView(context: Context, attributeSet: AttributeSet? = null) : RelativeLayout(context, attributeSet) {

    private var binding: TokenViewLayoutBinding = TokenViewLayoutBinding.bind(
        inflate(context, R.layout.token_view_layout, this)
    )

    fun initView(
        account: Account,
        callback: TokenViewCallback,
        assetIndex: Int = Int.InvalidIndex,
        logoRes: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_default_token)
    ) {
        prepareView(account, assetIndex, logoRes)
        prepareListeners(callback, account, assetIndex)
        getTokensValues(account, assetIndex).let { (crypto, fiat) ->
            with(binding.amountView) {
                setCrypto(getCryptoBalance(crypto))
                setFiat(getFiatBalance(fiat))
            }
        }
    }

    private fun getTokensValues(account: Account, assetIndex: Int): Pair<BigDecimal, BigDecimal> =
        if (assetIndex != Int.InvalidIndex) Pair(account.accountAssets[assetIndex].balance, WRONG_CURRENCY_VALUE)
        else Pair(account.cryptoBalance, prepareFiatBalance(account))

    private fun prepareFiatBalance(account: Account) =
        if (account.network.testNet) BigDecimal.ZERO
        else account.fiatBalance

    private fun prepareView(account: Account, assetIndex: Int, logo: Drawable?) {
        binding.apply {
            tokenLogo.setImageDrawable(logo)
            tokenName.text = if (assetIndex != Int.InvalidIndex) account.accountAssets[assetIndex].let { it.asset.name }
            else account.network.token
        }
    }


    private fun prepareListeners(callback: TokenViewCallback, account: Account, assetIndex: Int) {
        if (assetIndex != Int.InvalidIndex) this.setOnClickListener { callback.onSendTokenAssetClicked(account.index, assetIndex) }
        else this.setOnClickListener { callback.onSendTokenClicked(account) }
    }

    companion object {
        private val WRONG_CURRENCY_VALUE = (-1).toBigDecimal()
    }

    interface TokenViewCallback {
        fun onSendTokenAssetClicked(accountIndex: Int, assetIndex: Int)
        fun onSendTokenClicked(account: Account)
        val viewGroup: ViewGroup
        val context: Context
    }
}