package minerva.android.walletmanager.model.token

import minerva.android.walletmanager.model.minervaprimitives.account.Account
import java.math.BigDecimal

data class ERCTokensList(
    private val list: List<AccountToken>
) {
    private fun getCollectibles() = list.filter { accountToken -> accountToken.token.type.isNft() }

    fun getCollectionsWithBalance(account: Account) = getCollectibles()
        .map { accountToken -> accountToken to account.getTokenBalance(accountToken, accountToken.token.type) }
        .distinctBy { pair -> pair.first.token.address }
        .sortedByDescending { pair -> pair.second }

    fun getERC20Tokens() = list.filter { accountToken -> accountToken.token.type.isERC20() }

    fun isERC20TokensListNotEmpty() = getERC20Tokens().isNotEmpty()

    fun isCollectiblesListNotEmpty() = getCollectibles().isNotEmpty()

    fun isNotEmpty() = list.isNotEmpty()

    private fun Account.getERC1155Balance(accountToken: AccountToken) = accountTokens
        .filter { token ->
            token.token.address.equals(
                accountToken.token.address,
                true
            )
        }
        .fold(BigDecimal.ZERO) { acc, e ->
            acc + BigDecimal.ONE
        }

    private fun Account.getTokenBalance(accountToken: AccountToken) = accountTokens
        .find { token -> token.token.address.equals(accountToken.token.address, true) }
        ?.currentBalance
        ?: BigDecimal.ZERO

    private fun Account.getTokenBalance(accountToken: AccountToken, type: TokenType) = when {
        type.isERC1155() -> getERC1155Balance(accountToken)
        else -> getTokenBalance(accountToken)
    }
}