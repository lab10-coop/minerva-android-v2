package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokenBalance
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token

object TokenBalanceToERC20Token {
    fun map(chainId: Int, token: TokenBalance): ERC20Token =
        ERC20Token(chainId, token.name, token.symbol, token.address, token.decimals)
}