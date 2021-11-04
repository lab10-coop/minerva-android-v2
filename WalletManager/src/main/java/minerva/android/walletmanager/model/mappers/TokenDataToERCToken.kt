package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokenData
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.token.Tokens

object TokenDataToERCToken {
    fun map(chainId: Int, token: TokenData, accountAddress: String): ERCToken {
        val tokenType = mapToTokenType(token.type)
        return ERCToken(
            chainId = chainId,
            name = if (tokenType.isERC20()) token.name else String.Empty,
            symbol = token.symbol,
            address = token.address,
            decimals = token.decimals,
            accountAddress = accountAddress,
            type = tokenType,
            collectionName = if (tokenType.isERC721()) token.name else null
        )
    }

    private fun mapToTokenType(type: String): TokenType =
        when (type) {
            Tokens.ERC_20.type -> TokenType.ERC20
            Tokens.ERC_721.type -> TokenType.ERC721
            else -> TokenType.INVALID
        }
}