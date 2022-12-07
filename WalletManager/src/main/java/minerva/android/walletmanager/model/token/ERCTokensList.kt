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
        .sortedWith(
            compareBy( { !it.first.token.logoURI.isNullOrEmpty() }, { it.second } )
        )
        .reversed()

    fun getERC20Tokens() = list.filter { accountToken -> accountToken.token.type.isERC20() }

    fun isERC20TokensListNotEmpty() = getERC20Tokens().isNotEmpty()

    fun isCollectiblesListNotEmpty() = getCollectibles().isNotEmpty()

    fun isNotEmpty() = list.isNotEmpty()

    private fun Account.getNftBalance(accountToken: AccountToken): BigDecimal =
        accountTokens
        .filter { token ->
            token.token.address.equals(
                accountToken.token.address,
                true
            )
        }
        .size
        .toBigDecimal()

    private fun Account.getTokenBalance(accountToken: AccountToken): BigDecimal = accountTokens
        .find { token -> token.token.address.equals(accountToken.token.address, true) }
        ?.currentBalance
        ?: BigDecimal.ZERO

    private fun Account.getTokenBalance(accountToken: AccountToken, type: TokenType): BigDecimal = when {
        type.isNft() -> getNftBalance(accountToken)
        else -> getTokenBalance(accountToken)
    }
}