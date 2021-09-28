package minerva.android.widget.token

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import minerva.android.R
import minerva.android.databinding.TokenViewBinding
import minerva.android.kotlinUtils.InvalidIndex
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
        accountToken: AccountToken? = null
    ) {
        prepareView(accountToken, account)
        prepareListeners(callback, account, accountToken)
        getTokensValues(account, accountToken).let { (currentBalance, fiatBalance, nextBalance) ->
            with(binding.amountView) {
                setCryptoBalance(getCryptoBalance(currentBalance))
                if (accountToken != null) {
                    updateTokenBalance(accountToken, currentBalance, nextBalance)
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
                setFiat(getFiatBalance(fiatBalance, fiatSymbol))
            }
        }
    }

    private fun CryptoAmountView.updateTokenBalance(
        accountToken: AccountToken,
        currentBalance: BigDecimal,
        nextBalance: BigDecimal
    ) {
        when {
            isInitStream(accountToken) -> initAnimation(accountToken, currentBalance)
            isStreamableToken(accountToken) ->
                startStreamingAnimation(currentBalance, nextBalance, accountToken.token.consNetFlow)
        }
    }

    private fun CryptoAmountView.initAnimation(
        accountToken: AccountToken,
        currentBalance: BigDecimal
    ) {
        /*Distinguish between Fraction and other tokens is needed because of the extended number of digits for that token to show the animation*/
        if (accountToken.token.address == FRACTION_ADDRESS) {
            startStreamingAnimation(
                currentBalance,
                getInitStreamNextBalance(
                    accountToken.token.consNetFlow,
                    currentBalance,
                    NEXT_FRACTION_CRYPTO_BALANCE
                ),
                INIT_NET_FLOW
            )
        } else {
            startStreamingAnimation(
                currentBalance,
                getInitStreamNextBalance(
                    accountToken.token.consNetFlow, currentBalance,
                    NEXT_CRYPTO_BALANCE
                ),
                INIT_NET_FLOW
            )
        }
    }

    private fun getInitStreamNextBalance(
        netFlow: BigInteger,
        currentBalance: BigDecimal,
        nextCryptoBalance: BigDecimal
    ): BigDecimal =
        if (netFlow.signum() == NEGATIVE) currentBalance.minus(nextCryptoBalance)
        else currentBalance.plus(nextCryptoBalance)

    private fun isStreamableToken(accountToken: AccountToken): Boolean =
        accountToken.token.isStreamActive && accountToken.token.consNetFlow != BigInteger.ZERO

    private fun isInitStream(accountToken: AccountToken): Boolean =
        accountToken.isInitStream && accountToken.token.consNetFlow != BigInteger.ZERO

    private fun getTokensValues(
        account: Account,
        accountToken: AccountToken?
    ): Triple<BigDecimal, BigDecimal, BigDecimal> =
        if (accountToken != null) {
            with(accountToken) {
                Triple(currentBalance, fiatBalance, nextBalance)
            }
        } else {
            Triple(account.cryptoBalance, prepareFiatBalance(account), BigDecimal.ZERO)
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

    fun endStreamAnimation() {
        binding.amountView.endStreamAnimation()
    }

    interface TokenViewCallback {
        fun onSendTokenClicked(account: Account, tokenAddress: String, isTokenError: Boolean)
        fun onSendCoinClicked(account: Account)
    }

    companion object {
        private val NEXT_CRYPTO_BALANCE = BigDecimal(0.0000001)
        private val NEXT_FRACTION_CRYPTO_BALANCE = BigDecimal(0.00000000000001)
        private val INIT_NET_FLOW: BigInteger = BigInteger.valueOf(11574074074)
        private const val FRACTION_ADDRESS: String = "0x2bf2ba13735160624a0feae98f6ac8f70885ea61"
    }
}