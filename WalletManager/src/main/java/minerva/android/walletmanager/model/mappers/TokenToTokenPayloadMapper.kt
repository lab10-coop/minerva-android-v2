package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.TokenPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Token

object TokenToTokenPayloadMapper : Mapper<Token, TokenPayload> {
    override fun map(input: Token): TokenPayload =
        TokenPayload(input.name, input.symbol, input.address, input.decimals)
}