package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokenData
import minerva.android.walletmanager.model.token.ERC20Token

object TokenDataToERC20Token {
    fun map(chainId: Int, token: TokenData, accountAddress: String): ERC20Token =
        ERC20Token(
            chainId = chainId,
            name = token.name,
            symbol = token.symbol,
            address = token.address,
            decimals = token.decimals,
            accountAddress = accountAddress
        )
}