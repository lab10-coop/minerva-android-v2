package minerva.android.walletmanager.model.mappers

import minerva.android.configProvider.model.walletConfig.ERC20TokenPayload
import minerva.android.kotlinUtils.Mapper
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.Token

object TokenPayloadToTokenMapper : Mapper<ERC20TokenPayload, ERC20Token> {
    override fun map(input: ERC20TokenPayload): ERC20Token =
        ERC20Token(input.chainId, input.name, input.symbol, input.address, input.decimals)
}