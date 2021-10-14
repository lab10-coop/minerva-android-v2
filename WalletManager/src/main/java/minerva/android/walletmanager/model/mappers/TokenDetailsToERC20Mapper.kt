package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokenDetails
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenTag
import minerva.android.walletmanager.model.token.TokenType

object TokenDetailsToERC20TokensMapper : Mapper<List<TokenDetails>, List<ERCToken>> {
    override fun map(input: List<TokenDetails>): List<ERCToken> =
        mutableListOf<ERCToken>().apply {
            input.forEach {
                add(TokenDetailsToERC20Mapper.map(it))
            }
        }
}

object TokenDetailsToERC20Mapper : Mapper<TokenDetails, ERCToken> {
    override fun map(input: TokenDetails): ERCToken = with(input) {
        ERCToken(
            chainId = chainId,
            name = name,
            symbol = symbol,
            address = address,
            decimals = decimals.toString(),
            logoURI = logoURI,
            tag = tags.first(),
            type = parseERC20TokenType(tags.first())
        )
    }

    private fun parseERC20TokenType(tag: String) =
        when (tag) {
            TokenTag.SUPER_TOKEN.name -> TokenType.SUPER_TOKEN
            TokenTag.WRAPPER_TOKEN.name -> TokenType.WRAPPER_TOKEN
            else -> TokenType.ERC20
        }
}