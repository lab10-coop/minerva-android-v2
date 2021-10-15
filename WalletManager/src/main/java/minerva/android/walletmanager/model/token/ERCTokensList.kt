package minerva.android.walletmanager.model.token

import minerva.android.walletmanager.model.minervaprimitives.account.Account
import java.math.BigDecimal

data class ERCTokensList(
    private val list: List<AccountToken>
) {
    private fun getCollectibles() = list.filter { accountToken -> accountToken.token.type.isERC721() }

    fun getCollectiblesWithBalance(account: Account) = getCollectibles()
        .distinctBy { accountToken -> accountToken.token.address }
        .map { accountToken ->
            val balance =
                account.accountTokens.find { token -> token.token.address == accountToken.token.address }?.currentRawBalance
                    ?: BigDecimal.ZERO
            accountToken to balance
        }
        .sortedByDescending { pair -> pair.second }

    fun getERC20Tokens() = list.filter { accountToken -> accountToken.token.type.isERC20() }

    fun isERC20TokensListNotEmpty() = getERC20Tokens().isNotEmpty()

    fun isCollectiblesListNotEmpty() = getCollectibles().isNotEmpty()

    fun isNotEmpty() = list.isNotEmpty()
}