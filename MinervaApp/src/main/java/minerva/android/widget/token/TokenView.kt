package minerva.android.widget.token

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import minerva.android.R
import minerva.android.databinding.TokenViewBinding
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.NativeToken
import minerva.android.walletmanager.utils.BalanceUtils.getCryptoBalance
import minerva.android.walletmanager.utils.BalanceUtils.getFiatBalance
import minerva.android.widget.repository.getMainTokenIconRes
import java.math.BigDecimal
import java.math.BigInteger

class TokenView(context: Context, attributeSet: AttributeSet? = null) :
    RelativeLayout(context, attributeSet) {

    private var binding: TokenViewBinding =
        TokenViewBinding.bind(inflate(context, R.layout.token_view, this))

    fun initView(
        account: Account,
        callback: TokenViewCallback,
        fiatSymbol: String,
        accountToken: AccountToken? = null
    ) {
        prepareView(accountToken, account)
        prepareListeners(callback, account, accountToken)
        getTokensValues(account, accountToken).let { (currentBalance, fiatBalance) ->
            with(binding.amountView) {
                setCryptoBalance(getCryptoBalance(currentBalance))
                if (accountToken != null && accountToken.token.consNetFlow != BigInteger.ZERO) {
                    setStreamingValues(currentBalance, accountToken.token.consNetFlow)
                    startStreamAnimation()
                }

                if (account.isError) {
                    setErrorColor()
                }

                accountToken?.token?.let {
                    if (it.isError) {
                        endStreamAnimation()
                        setErrorColor()
                    }
                }
                setFiat(getFiatBalance(fiatBalance, fiatSymbol, rounding = true))
            }
        }
    }

    private fun getTokensValues(
        account: Account,
        accountToken: AccountToken?
    ): Pair<BigDecimal, BigDecimal> =
        if (accountToken != null) {
            with(accountToken) {
                Pair(currentBalance, fiatBalance)
            }
        } else {
            Pair(account.cryptoBalance, prepareFiatBalance(account))
        }

    private fun prepareFiatBalance(account: Account) =
        if (account.network.testNet) BigDecimal.ZERO
        else account.fiatBalance

    private fun prepareView(accountToken: AccountToken?, account: Account) {
        binding.apply {
            if (accountToken != null) {
                with(accountToken) {
                    tokenLogo.initView(this.token)
                    tokenName.text = token.symbol
                }
            } else {
                with(account.network) {
                    tokenLogo.initView(
                        NativeToken(
                            chainId,
                            name,
                            token,
                            getMainTokenIconRes(chainId)
                        )
                    )
                    tokenName.text = token
                }
            }
        }
    }

    private fun prepareListeners(
        callback: TokenViewCallback,
        account: Account,
        accountToken: AccountToken?
    ) {
        if (accountToken != null) setOnClickListener {
            callback.onSendTokenClicked(
                account,
                accountToken.token.address,
                accountToken.token.isError
            )
        }
        else setOnClickListener { callback.onSendCoinClicked(account) }
    }

    fun startStreamAnimation() {
        binding.amountView.startStreamAnimation()
    }

    fun endStreamAnimation() {
        binding.amountView.endStreamAnimation()
    }

    interface TokenViewCallback {
        fun onSendTokenClicked(account: Account, tokenAddress: String, isTokenError: Boolean)
        fun onSendCoinClicked(account: Account)
    }
}