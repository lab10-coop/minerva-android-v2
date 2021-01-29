package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token

object ERC20TokenPayloadToERC20TokenMapper : Mapper<ERC20TokenPayload, ERC20Token> {
    override fun map(input: ERC20TokenPayload): ERC20Token =
        with(input) {
            ERC20Token(chainId, name, symbol, address, decimals, logoURI)
        }
}