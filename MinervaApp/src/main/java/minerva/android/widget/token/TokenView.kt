package minerva.android.widget.token

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import minerva.android.R
import minerva.android.databinding.TokenViewBinding
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.NativeToken
import minerva.android.walletmanager.utils.BalanceUtils.getCryptoBalance
import minerva.android.walletmanager.utils.BalanceUtils.getFiatBalance
import minerva.android.widget.repository.getMainTokenIconRes
import java.math.BigDecimal

class TokenView(context: Context, attributeSet: AttributeSet? = null) : RelativeLayout(context, attributeSet) {

    private var binding: TokenViewBinding = TokenViewBinding.bind(inflate(context, R.layout.token_view, this))

    fun initView(
        account: Account,
        callback: TokenViewCallback,
        fiatSymbol: String,
        token: ERC20Token? = null
    ) {
        prepareView(token, account)
        prepareListeners(callback, account, token)

        getTokensValues(account, token).let { (crypto, fiat) ->
            with(binding.amountView) {
                setCryptoBalance(getCryptoBalance(crypto))
                if (account.isError) {
                    setErrorColor()
                }
                token?.let {
                    if (it.isError) {
                        setErrorColor()
                    }
                }
                setFiat(getFiatBalance(fiat, fiatSymbol))
            }
        }
    }

    private fun getTokensValues(account: Account, token: ERC20Token?): Pair<BigDecimal, BigDecimal> =
        if (token != null) {
            with(getToken(account, token.address)) {
                Pair(balance, fiatBalance)
            }
        } else Pair(account.cryptoBalance, prepareFiatBalance(account))

    private fun prepareFiatBalance(account: Account) =
        if (account.network.testNet) BigDecimal.ZERO
        else account.fiatBalance

    private fun prepareView(erc20token: ERC20Token?, account: Account) {
        binding.apply {
            if (erc20token != null)
                with(erc20token) {
                    tokenLogo.initView(this)
                    tokenName.text = symbol
                }
            else
                with(account.network) {
                    tokenLogo.initView(NativeToken(chainId, name, token, getMainTokenIconRes(chainId)))
                    tokenName.text = token
                }
        }
    }

    private fun prepareListeners(callback: TokenViewCallback, account: Account, token: ERC20Token?) {
        if (token != null) setOnClickListener { callback.onSendTokenClicked(account, token.address, token.isError) }
        else setOnClickListener { callback.onSendCoinClicked(account) }
    }

    private fun getToken(account: Account, tokenAddress: String): AccountToken =
        account.accountTokens.find { it.token.address == tokenAddress } ?: AccountToken(ERC20Token(Int.InvalidValue))

    interface TokenViewCallback {
        fun onSendTokenClicked(account: Account, tokenAddress: String, isTokenError: Boolean)
        fun onSendCoinClicked(account: Account)
    }
}