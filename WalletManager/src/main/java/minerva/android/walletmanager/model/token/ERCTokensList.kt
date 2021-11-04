package minerva.android.walletmanager.model.token

import minerva.android.walletmanager.model.minervaprimitives.account.Account
import java.math.BigDecimal

data class ERCTokensList(
    private val list: List<AccountToken>
) {
    private fun getCollectibles() = list.filter { accountToken -> accountToken.token.type.isERC721() }

    fun getCollectionsWithBalance(account: Account) = getCollectibles()
        .map { accountToken ->
            val balance =
                account.accountTokens.filter { token -> token.token.address.equals(accountToken.token.address, true) }
                    .sumBy { token -> token.currentRawBalance.intValueExact() }
            accountToken to BigDecimal(balance)
        }
        .distinctBy { pair -> pair.first.token.address }
        .sortedByDescending { pair -> pair.second }

    fun getERC20Tokens() = list.filter { accountToken -> accountToken.token.type.isERC20() }

    fun isERC20TokensListNotEmpty() = getERC20Tokens().isNotEmpty()

    fun isCollectiblesListNotEmpty() = getCollectibles().isNotEmpty()

    fun isNotEmpty() = list.isNotEmpty()
}