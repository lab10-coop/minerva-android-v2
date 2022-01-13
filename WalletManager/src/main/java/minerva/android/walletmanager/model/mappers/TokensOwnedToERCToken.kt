package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokensOwnedPayload
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.token.Tokens

object TokensOwnedToERCToken {
    fun map(chainId: Int, token: TokensOwnedPayload.TokenOwned, accountAddress: String): ERCToken {
        val tokenType = mapToTokenType(token.types)
        return ERCToken(
            chainId = chainId,
            name = if (tokenType.isERC20()) token.name ?: String.Empty else String.Empty,
            symbol = token.symbol ?: String.Empty,
            address = token.contractAddress,
            decimals = token.decimals,
            accountAddress = accountAddress,
            type = tokenType,
            collectionName = if (tokenType.isNft()) token.name ?: String.Empty else null,
            tokenId = if (tokenType.isNft()) token.id else null
        )
    }

    private fun mapToTokenType(types: List<String>): TokenType = when {
        types.contains(Tokens.ERC_1155.type) -> TokenType.ERC1155
        types.contains(Tokens.ERC_721.type) -> TokenType.ERC721
        types.contains(Tokens.ERC_20.type) -> TokenType.ERC20
        else -> TokenType.INVALID
    }
}