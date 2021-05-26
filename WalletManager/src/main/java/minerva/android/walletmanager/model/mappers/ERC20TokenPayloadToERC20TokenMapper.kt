package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.token.ERC20Token

object ERC20TokenPayloadToERC20TokenMapper : Mapper<ERC20TokenPayload, ERC20Token> {
    override fun map(input: ERC20TokenPayload): ERC20Token =
        with(input) {
            ERC20Token(
                chainId = chainId,
                name = name,
                symbol = symbol,
                address = address,
                decimals = decimals,
                accountAddress = accountAddress,
                logoURI = logoURI,
                tag = input.tag
            )
        }
}