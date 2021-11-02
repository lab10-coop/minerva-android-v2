package minerva.android.walletmanager.model.minervaprimitives.account

import minerva.android.walletmanager.model.token.AccountToken
import java.math.BigDecimal

interface Asset {
    val chainId: Int
    val privateKey: String
}

data class AssetBalance(
    override val chainId: Int,
    override val privateKey: String,
    val accountToken: AccountToken
) : Asset {
    val accountAddress: String get() = accountToken.token.accountAddress
    val tokenAddress: String get() = accountToken.token.address
    val currentBalance: BigDecimal get() = accountToken.currentBalance
}

data class AssetError(
    override val chainId: Int,
    override val privateKey: String,
    val accountAddress: String,
    val tokenAddress: String,
    val error: Throwable,
    val tokenId: String?
) : Asset