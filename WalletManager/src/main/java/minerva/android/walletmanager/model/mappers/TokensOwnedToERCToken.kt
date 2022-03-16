package minerva.android.walletmanager.model.mappers

import android.util.Base64
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
            applyPayload(token)
        }
    }

    private fun mapToTokenType(types: List<String>): TokenType = when {
        types.contains(Tokens.ERC_1155.type) -> TokenType.ERC1155
        types.contains(Tokens.ERC_721.type) -> TokenType.ERC721
        types.contains(Tokens.ERC_20.type) -> TokenType.ERC20
        else -> TokenType.INVALID
    }

    private fun ERCToken.applyPayload(token: TokensOwnedPayload.TokenOwned) {
        token.tokenJson?.let { handleTokenJson(it) }
        nftContent.tokenUri = handleTokenUri(token)
    }

    private fun ERCToken.handleTokenJson(tokenJson: TokensOwnedPayload.TokenOwned.TokenJson) {
        val type = when {
            !tokenJson.animationUri.isNullOrBlank() -> ContentType.VIDEO
            tokenJson.image?.startsWith(ENCODED_SVG_PREFIX) ?: false -> {
                tokenJson.image?.let {
                    val encodedString = it.removePrefix(ENCODED_SVG_PREFIX)
                    val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
                    val decodedString = String(decodedBytes)
                    tokenJson.image = decodedString
                }
                ContentType.ENCODED_IMAGE
            }
            else -> ContentType.IMAGE
        }
        nftContent = NftContent(
            parseIPFSContentUrl(tokenJson.image ?: String.Empty),
            type,
            parseIPFSContentUrl(tokenJson.animationUri ?: String.Empty),
            background = tokenJson.backgroundColor ?: String.Empty,
            description = tokenJson.description ?: String.Empty
        )
        tokenJson.name?.let { name = it }
    }

    private fun handleTokenUri(token: TokensOwnedPayload.TokenOwned) =  token.tokenURI?.takeIf { !it.startsWith(ENCODED_TOKEN_URI_PREFIX) } ?: String.Empty

    private const val ENCODED_SVG_PREFIX = "data:image/svg+xml;base64,"
    private const val ENCODED_TOKEN_URI_PREFIX = "data:application/json;base64,"
}