package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.TokenPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.Token

object TokenPayloadToTokenMapper : Mapper<TokenPayload, Token> {
    override fun map(input: TokenPayload): Token =
        Token(input.name, input.symbol, input.address, input.decimals)
}