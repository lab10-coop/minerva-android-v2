package minerva.android.walletmanager.model.mappers

import minerva.android.apiProvider.model.TokenBalance
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token

object TokenBalanceToAccountToken {
    fun map(network: String, token: TokenBalance): AccountToken =
        AccountToken(
            ERC20Token(
                NetworkManager.getChainId(network), token.name, token.symbol, token.address, token.decimals),
                token.balance.toBigDecimal()
            )
}