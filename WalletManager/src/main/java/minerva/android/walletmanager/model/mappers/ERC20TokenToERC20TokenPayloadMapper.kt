package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.token.ERC20Token

object ERC20TokenToERC20TokenPayloadMapper : Mapper<ERC20Token, ERC20TokenPayload> {
    override fun map(input: ERC20Token): ERC20TokenPayload =
        with(input) {
            ERC20TokenPayload(
                chainId = chainId,
                name = name,
                symbol = symbol,
                address = address,
                decimals = decimals,
                logoURI = logoURI,
                accountAddress = accountAddress,
                tag = tag
            )
        }
}