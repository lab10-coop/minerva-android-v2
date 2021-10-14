package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.kotlinUtils.Mapper
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
                collectionName = collectionName
            )
        }
}