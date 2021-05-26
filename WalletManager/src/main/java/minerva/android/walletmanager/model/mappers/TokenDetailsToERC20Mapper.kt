package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokenDetails
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.token.ERC20Token

object TokenDetailsToERC20TokensMapper : Mapper<List<TokenDetails>, List<ERC20Token>> {
    override fun map(input: List<TokenDetails>): List<ERC20Token> =
        mutableListOf<ERC20Token>().apply {
            input.forEach {
                add(TokenDetailsToERC20Mapper.map(it))
            }
        }
}

object TokenDetailsToERC20Mapper : Mapper<TokenDetails, ERC20Token> {
    override fun map(input: TokenDetails): ERC20Token = with(input) {
        ERC20Token(
            chainId = chainId,
            name = name,
            symbol = symbol,
            address = address,
            decimals = decimals.toString(),
            logoURI = logoURI,
            tag = tags.first()
        )
    }
}