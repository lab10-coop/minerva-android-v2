package minerva.android.walletmanager.model.minervaprimitives.account

import minerva.android.walletmanager.model.token.AccountToken

data class TokenBalance(
    val chainId: Int,
    val privateKey: String,
    val accountTokenList: List<AccountToken>
)