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
    var tokenPrice: Double? = Double.InvalidValue,
    var underlyingPrices: List<Double> = emptyList()
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
        get() {
            if (tokenPrice != Double.InvalidValue) {
                return tokenPrice
                    ?.let { BigDecimal(it).multiply(currentBalance).setScale(FIAT_SCALE, RoundingMode.HALF_UP) }
                    .orElse { Double.InvalidValue.toBigDecimal() }
            }
            if (underlyingPrices.isNotEmpty()) {
                // todo: improve this, average is not correct, should be the sum and mutliply to get 100%
                return token.underlyingBalances?.let {
                    underlyingPrices
                        .zip(it)
                        .filter { (underlyingPrice, underlyingBalance ) -> underlyingPrice != Double.InvalidValue }
                        .map { (underlyingPrice, underlyingBalance) ->
                            BigDecimal(underlyingPrice)
                                .multiply(BigDecimal(underlyingBalance))
                                .setScale(FIAT_SCALE, RoundingMode.HALF_UP)
                        }
                        .reduce { acc, fiatBalance -> acc.add(fiatBalance) }
                        // todo: assumes same percentage, fix this
                        .multiply(
                            BigDecimal(
                                underlyingPrices.size /
                                underlyingPrices
                                .filter { underlyingPrice -> underlyingPrice != Double.InvalidValue }
                                .size
                            )
                        )
                }.orElse { Double.InvalidValue.toBigDecimal() }
            }
            return Double.InvalidValue.toBigDecimal()
        }



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