package minerva.android.widget.token

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import minerva.android.R
import minerva.android.databinding.TokenViewBinding
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.utils.BalanceUtils.getCryptoBalance
import minerva.android.utils.BalanceUtils.getFiatBalance
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.token.NativeToken
import minerva.android.widget.repository.getMainTokenIconRes
import java.math.BigDecimal

class TokenView(context: Context, attributeSet: AttributeSet? = null) : RelativeLayout(context, attributeSet) {

    private var binding: TokenViewBinding = TokenViewBinding.bind(
        inflate(context, R.layout.token_view, this)
    )

    fun initView(
        account: Account,
        callback: TokenViewCallback,
        tokenIndex: Int = Int.InvalidIndex
    ) {
        prepareView(account, tokenIndex)
        prepareListeners(callback, account, tokenIndex)
        getTokensValues(account, tokenIndex).let { (crypto, fiat) ->
            with(binding.amountView) {
                setCrypto(getCryptoBalance(crypto))
                setFiat(getFiatBalance(fiat))
            }
        }
    }

    private fun getTokensValues(account: Account, tokenIndex: Int): Pair<BigDecimal, BigDecimal> =
        if (tokenIndex != Int.InvalidIndex) Pair(account.accountTokens[tokenIndex].balance, WRONG_CURRENCY_VALUE)
        else Pair(account.cryptoBalance, prepareFiatBalance(account))

    private fun prepareFiatBalance(account: Account) =
        if (account.network.testNet) BigDecimal.ZERO
        else account.fiatBalance

    private fun prepareView(account: Account, tokenIndex: Int) {
        binding.apply {
            if (tokenIndex != Int.InvalidIndex)
                with(account.accountTokens[tokenIndex].token) {
                    tokenLogo.initView(this)
                    tokenName.text = symbol
                }
            else
                with(account.network) {
                    tokenLogo.initView(NativeToken(chainId, full, token, getMainTokenIconRes(short)))
                    tokenName.text = token
                }
        }
    }


    private fun prepareListeners(callback: TokenViewCallback, account: Account, tokenIndex: Int) {
        if (tokenIndex != Int.InvalidIndex) setOnClickListener { callback.onSendTokenTokenClicked(account, tokenIndex) }
        else setOnClickListener { callback.onSendTokenClicked(account) }
    }

    companion object {
        private val WRONG_CURRENCY_VALUE = (-1).toBigDecimal()
    }

    interface TokenViewCallback {
        fun onSendTokenTokenClicked(account: Account, tokenIndex: Int)
        fun onSendTokenClicked(account: Account)
    }
}