package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokensOwnedPayload
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.ContentType
import minerva.android.walletmanager.model.NftContent
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.token.Tokens
import minerva.android.walletmanager.utils.parseIPFSContentUrl

object TokensOwnedToERCToken {

    fun map(chainId: Int, token: TokensOwnedPayload.TokenOwned, accountAddress: String): ERCToken {
        val tokenType = mapToTokenType(token.types)
        return ERCToken(
            chainId = chainId,
            name = if (tokenType.isERC20()) token.name else String.Empty,
            symbol = token.symbol,
            address = token.contractAddress,
            decimals = token.decimals,
            accountAddress = accountAddress,
            type = tokenType,
            collectionName = if (tokenType.isNft()) token.name else null,
            tokenId = if (tokenType.isNft()) token.id else null
        ).apply {
            token.tokenJson?.let {
                handleNftContent(it)
            }
            nftContent.tokenUri = token.tokenURI
        }
    }

    private fun mapToTokenType(types: List<String>): TokenType = when {
        types.contains(Tokens.ERC_1155.type) -> TokenType.ERC1155
        types.contains(Tokens.ERC_721.type) -> TokenType.ERC721
        types.contains(Tokens.ERC_20.type) -> TokenType.ERC20
        else -> TokenType.INVALID
    }



    private fun ERCToken.handleNftContent(tokenJson: TokensOwnedPayload.TokenOwned.TokenJson)  {
        val type = when {
            !tokenJson.animationUri.isNullOrBlank() -> ContentType.VIDEO
            else -> ContentType.IMAGE
        }
        nftContent = NftContent(
            parseIPFSContentUrl(tokenJson.image ?: String.Empty),
            type,
            parseIPFSContentUrl(tokenJson.animationUri ?: String.Empty))
        tokenJson.description?.let{description = it}
        tokenJson.name?.let{name = it}
    }
}