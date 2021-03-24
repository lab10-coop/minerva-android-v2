package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokenData
import minerva.android.walletmanager.model.token.ERC20Token

object TokenDataToERC20Token {
    fun map(chainId: Int, token: TokenData): ERC20Token =
        ERC20Token(chainId, token.name, token.symbol, token.address, token.decimals)
}