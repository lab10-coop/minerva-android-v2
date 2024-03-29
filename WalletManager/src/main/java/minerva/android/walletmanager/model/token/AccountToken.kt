package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.ContentType
import minerva.android.walletmanager.utils.BalanceUtils
import java.math.BigDecimal
import java.math.RoundingMode

data class AccountToken(
    override var token: ERCToken,
    var rawBalance: BigDecimal = Double.InvalidValue.toBigDecimal(),
    var tokenPrice: Double? = Double.InvalidValue
) : TokenWithBalances {

    override fun equals(other: Any?): Boolean =
        (other as? AccountToken)
            ?.let { accountToken -> token.address.equals(accountToken.token.address, true) }
            .orElse { false }

    override val currentBalance: BigDecimal
        get() = if (token.type.isERC721() || token.decimals.isBlank()) rawBalance else getBalanceForTokenWithDecimals(
            rawBalance
        )

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

    fun mergeNftDetailsAfterWalletConfigUpdate(ercToken: ERCToken){
        mergePropertiesWithLocalFirstStrategy(ercToken)
        mergePropertiesWithRemoteFirstStrategy(ercToken)
    }

    private fun mergePropertiesWithLocalFirstStrategy(ercToken: ERCToken){
        token.logoURI = ercToken.logoURI
        token.collectionName = ercToken.collectionName
        token.symbol = ercToken.symbol
        token.name = ercToken.name
    }

    private fun mergePropertiesWithRemoteFirstStrategy(ercToken: ERCToken){
        if(token.nftContent.imageUri.isEmpty()) token.nftContent.imageUri = ercToken.nftContent.imageUri
        if(token.nftContent.contentType == ContentType.INVALID) token.nftContent.contentType = ercToken.nftContent.contentType
        if(token.nftContent.animationUri.isEmpty()) token.nftContent.animationUri = ercToken.nftContent.animationUri
        if(token.nftContent.background.isEmpty()) token.nftContent.background = ercToken.nftContent.background
        if(token.nftContent.tokenUri.isEmpty()) token.nftContent.tokenUri = ercToken.nftContent.tokenUri
        if(token.nftContent.description.isEmpty()) token.nftContent.description = ercToken.nftContent.description
    }

    companion object {
        private const val FIAT_SCALE = 13
    }
}