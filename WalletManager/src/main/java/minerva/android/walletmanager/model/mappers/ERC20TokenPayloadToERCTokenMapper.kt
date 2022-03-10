package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.configProvider.model.walletConfig.NftContentPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.ContentType
import minerva.android.walletmanager.model.NftContent
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenTag
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.valueOf

object ERC20TokenPayloadToERCTokenMapper : Mapper<ERC20TokenPayload, ERCToken> {
    override fun map(input: ERC20TokenPayload): ERCToken =
        with(input) {
            ERCToken(
                chainId = chainId,
                name = name,
                symbol = symbol,
                address = address,
                decimals = decimals,
                accountAddress = accountAddress,
                logoURI = logoURI,
                tag = input.tag,
                type = parseERCTokenType(input.tag, type),
                tokenId = tokenId,
                collectionName = collectionName,
                nftContent = parseNFTTokenPayload(nftContentPayload)
            )
        }

    private fun parseNFTTokenPayload(payload: NftContentPayload?): NftContent =
        payload?.run {
            NftContent(
                imageUri = imageUri,
                contentType = valueOf<ContentType>(contentType) ?: ContentType.INVALID,
                animationUri = animationUri,
                tokenUri = tokenUri,
                background = background,
                description = description
            )
        } ?: NftContent()

    private fun parseERCTokenType(tag: String, type: String) =
        when {
            tag == TokenTag.SUPER_TOKEN.name -> TokenType.SUPER_TOKEN
            tag == TokenTag.WRAPPER_TOKEN.name -> TokenType.WRAPPER_TOKEN
            type.isNotBlank() -> TokenType.valueOf(type)
            else -> TokenType.ERC20
        }
}