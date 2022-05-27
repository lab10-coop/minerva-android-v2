package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.configProvider.model.walletConfig.NftContentPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.NftContent
import minerva.android.walletmanager.model.token.ERCToken

object ERCTokenToERC20TokenPayloadMapper : Mapper<ERCToken, ERC20TokenPayload> {
    override fun map(input: ERCToken): ERC20TokenPayload =
        with(input) {
            ERC20TokenPayload(
                chainId = chainId,
                name = name,
                symbol = symbol,
                address = address,
                decimals = decimals,
                logoURI = logoURI,
                accountAddress = accountAddress,
                tag = tag,
                type = type.name,
                tokenId = tokenId,
                collectionName = collectionName,
                nftContentPayload = if(type.isNft()) parseNFTContent(nftContent) else null,
                isFavorite = isFavorite //isFavorite - used for nft status
            )
        }

    private fun parseNFTContent(content: NftContent): NftContentPayload =
        content.run {
            NftContentPayload(
                imageUri = imageUri,
                contentType = contentType.name,
                animationUri = animationUri,
                tokenUri = tokenUri,
                background = background,
                description = description
            )
        }
}