package minerva.android.widget.token

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import minerva.android.R
import minerva.android.databinding.TokenViewBinding
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
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
        tokenAddress: String = String.Empty
    ) {
        prepareView(account, tokenAddress)
        prepareListeners(callback, account, tokenAddress)
        getTokensValues(account, tokenAddress).let { (crypto, fiat) ->
            with(binding.amountView) {
                setCrypto(getCryptoBalance(crypto))
                setFiat(getFiatBalance(fiat))
            }
        }
    }

    private fun getTokensValues(account: Account, tokenAddress: String): Pair<BigDecimal, BigDecimal> =
        if (tokenAddress != String.Empty) Pair(
            getToken(account, tokenAddress).balance,
            getToken(account, tokenAddress).fiatBalance
        )
        else Pair(account.cryptoBalance, prepareFiatBalance(account))

    private fun prepareFiatBalance(account: Account) =
        if (account.network.testNet) BigDecimal.ZERO
        else account.fiatBalance

    private fun prepareView(account: Account, tokenAddress: String) {
        binding.apply {
            if (tokenAddress != String.Empty)
                with(getToken(account, tokenAddress).token) {
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

    private fun prepareListeners(callback: TokenViewCallback, account: Account, tokenAddress: String) {
        if (tokenAddress != String.Empty) setOnClickListener { callback.onSendTokenTokenClicked(account, tokenAddress) }
        else setOnClickListener { callback.onSendTokenClicked(account) }
    }

    private fun getToken(account: Account, tokenAddress: String) =
        account.accountTokens.find { it.token.address == tokenAddress } ?: AccountToken(ERC20Token(Int.InvalidValue))

    interface TokenViewCallback {
        fun onSendTokenTokenClicked(account: Account, tokenAddress: String)
        fun onSendTokenClicked(account: Account)
    }
}