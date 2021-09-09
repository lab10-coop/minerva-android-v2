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
import minerva.android.widget.CryptoAmountView
import minerva.android.widget.CryptoAmountView.Companion.NEGATIVE
import minerva.android.widget.repository.getMainTokenIconRes
import timber.log.Timber
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
        token: ERC20Token? = null
    ) {
        prepareView(token, account)
        prepareListeners(callback, account, token)
        getTokensValues(account, token).let { (currentBalance, fiatBalance, nextBalance) ->
            with(binding.amountView) {
                setCryptoBalance(getCryptoBalance(currentBalance))
                if (token != null) updateTokenBalance(account, token, currentBalance, nextBalance)
                if (account.isError) setErrorColor()
                token?.let {
                    if (it.isError) {
                        setErrorColor()
                    }
                }
                setFiat(getFiatBalance(fiatBalance, fiatSymbol))
            }
        }
    }

    private fun CryptoAmountView.updateTokenBalance(
        account: Account,
        token: ERC20Token,
        currentBalance: BigDecimal,
        nextBalance: BigDecimal
    ) {
        when {
            isInitStream(account, token) ->
                startStreamingAnimation(
                    currentBalance,
                    getInitStreamNextBalance(token.consNetFlow, currentBalance),
                    INIT_NET_FLOW
                )
            isStreamableToken(token) ->
                startStreamingAnimation(currentBalance, nextBalance, token.consNetFlow)
        }
    }

    private fun getInitStreamNextBalance(
        netFlow: BigInteger,
        currentBalance: BigDecimal
    ): BigDecimal =
        if (netFlow.signum() == NEGATIVE) currentBalance.minus(INIT_NEXT_CRYPTO_BALANCE)
        else currentBalance.plus(INIT_NEXT_CRYPTO_BALANCE)

    private fun isStreamableToken(token: ERC20Token): Boolean =
        token.isStreamActive && token.consNetFlow != BigInteger.ZERO

    private fun isInitStream(account: Account, token: ERC20Token): Boolean =
        getAccountToken(account, token.address).isInitStream && token.consNetFlow != BigInteger.ZERO

    private fun getTokensValues(
        account: Account,
        token: ERC20Token?
    ): Triple<BigDecimal, BigDecimal, BigDecimal> =
        if (token != null) {
            with(getAccountToken(account, token.address)) {
                Triple(currentBalance, fiatBalance, nextBalance)
            }
        } else {
            Triple(account.cryptoBalance, prepareFiatBalance(account), BigDecimal.ZERO)
        }

    private fun prepareFiatBalance(account: Account) =
        if (account.network.testNet) BigDecimal.ZERO
        else account.fiatBalance

    private fun prepareView(erc20token: ERC20Token?, account: Account) {
        binding.apply {
            if (erc20token != null) {
                with(erc20token) {
                    tokenLogo.initView(this)
                    tokenName.text = symbol
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
        token: ERC20Token?
    ) {
        if (token != null) setOnClickListener {
            callback.onSendTokenClicked(
                account,
                token.address,
                token.isError
            )
        }
        else setOnClickListener { callback.onSendCoinClicked(account) }
    }

    private fun getAccountToken(account: Account, tokenAddress: String): AccountToken =
        account.accountTokens.find { it.token.address == tokenAddress }
            ?: AccountToken(ERC20Token(Int.InvalidValue))

    fun endStreamAnimation() {
        binding.amountView.endStreamAnimation()
    }

    interface TokenViewCallback {
        fun onSendTokenClicked(account: Account, tokenAddress: String, isTokenError: Boolean)
        fun onSendCoinClicked(account: Account)
    }

    companion object {
        private val INIT_NEXT_CRYPTO_BALANCE = BigDecimal(0.0000001)
        private val INIT_NET_FLOW: BigInteger = BigInteger.valueOf(11574074074)
    }
}