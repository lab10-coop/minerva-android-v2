package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token

object TokenToTokenPayloadMapper : Mapper<ERC20Token, ERC20TokenPayload> {
    override fun map(input: ERC20Token): ERC20TokenPayload =
        ERC20TokenPayload(input.name, input.symbol, input.address, input.decimals)
}