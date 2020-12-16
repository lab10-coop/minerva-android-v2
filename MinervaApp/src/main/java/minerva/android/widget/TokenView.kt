package minerva.android.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import minerva.android.R
import minerva.android.databinding.TokenViewBinding
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.utils.BalanceUtils.getCryptoBalance
import minerva.android.utils.BalanceUtils.getFiatBalance
import minerva.android.walletmanager.model.Account
import java.math.BigDecimal

class TokenView(context: Context, attributeSet: AttributeSet? = null) : RelativeLayout(context, attributeSet) {

    private var binding: TokenViewBinding = TokenViewBinding.bind(
        inflate(context, R.layout.token_view, this)
    )

    fun initView(
        account: Account,
        callback: TokenViewCallback,
        assetIndex: Int = Int.InvalidIndex,
        logo: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_default_token)
    ) {
        prepareView(account, assetIndex, logo)
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

    private fun prepareView(account: Account, tokenIndex: Int, logo: Drawable?) {
        binding.apply {
            tokenLogo.setImageDrawable(logo)
            tokenName.text = if (tokenIndex != Int.InvalidIndex) account.accountAssets[tokenIndex].asset.name
            else account.network.token
        }
    }


    private fun prepareListeners(callback: TokenViewCallback, account: Account, tokenIndex: Int) {
        if (tokenIndex != Int.InvalidIndex) setOnClickListener { callback.onSendTokenAssetClicked(account.id, tokenIndex) }
        else setOnClickListener { callback.onSendTokenClicked(account) }
    }

    companion object {
        private val WRONG_CURRENCY_VALUE = (-1).toBigDecimal()
    }

    interface TokenViewCallback {
        fun onSendTokenAssetClicked(accountIndex: Int, tokenIndex: Int)
        fun onSendTokenClicked(account: Account)
    }
}