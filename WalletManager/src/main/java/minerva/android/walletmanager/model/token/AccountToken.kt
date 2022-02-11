package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.ContentType
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.RoundingMode

data class AccountToken(
    override var token: ERCToken,
    var currentRawBalance: BigDecimal = Double.InvalidValue.toBigDecimal(),
    var tokenPrice: Double? = Double.InvalidValue,
    var nextRawBalance: BigDecimal = Double.InvalidValue.toBigDecimal(),
    var isInitStream: Boolean = false
) : TokenWithBalances {

    override fun equals(other: Any?): Boolean =
        (other as? AccountToken)
            ?.let { accountToken -> token.address.equals(accountToken.token.address, true) }
            .orElse { false }

    override val currentBalance: BigDecimal
        get() = if (token.type.isERC721() || token.decimals.isBlank()) currentRawBalance else getBalanceForTokenWithDecimals(
            currentRawBalance
        )

    val nextBalance: BigDecimal
        get() = if (token.type.isERC721() || token.decimals.isBlank()) nextRawBalance else getBalanceForTokenWithDecimals(nextRawBalance)

    override val fiatBalance: BigDecimal
        get() =
            tokenPrice?.let { price ->
                when (price) {
                    Double.InvalidValue -> Double.InvalidValue.toBigDecimal()
                    else -> BigDecimal(price).multiply(currentBalance).setScale(FIAT_SCALE, RoundingMode.HALF_UP)
                }
            }.orElse { Double.InvalidValue.toBigDecimal() }


    private fun getBalanceForTokenWithDecimals(rawBalance: BigDecimal) =
        if (rawBalance == Double.InvalidValue.toBigDecimal()) rawBalance
        else BalanceUtils.convertFromWei(rawBalance, token.decimals.toInt())

    fun mergeNftDetails(ercToken: ERCToken){
        token.logoURI = ercToken.logoURI
        token.description = ercToken.description
        if(token.nftContent.imageUri.isEmpty()) token.nftContent.imageUri = ercToken.nftContent.imageUri
        if(token.nftContent.contentType == ContentType.INVALID) token.nftContent.contentType = ercToken.nftContent.contentType
        if(token.nftContent.animationUri.isEmpty()) token.nftContent.animationUri = ercToken.nftContent.animationUri
        if(token.nftContent.tokenUri.isEmpty()) token.nftContent.tokenUri = ercToken.nftContent.tokenUri
        token.name = ercToken.name
        token.collectionName = ercToken.collectionName
        token.symbol = ercToken.symbol
    }

    companion object {
        private const val FIAT_SCALE = 13
    }
}