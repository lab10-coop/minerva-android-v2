package minerva.android.walletmanager.model.minervaprimitives.account

import minerva.android.walletmanager.model.token.AccountToken

interface Asset {
    val chainId: Int
    val privateKey: String
}

data class AssetBalance(
    override val chainId: Int,
    override val privateKey: String,
    val accountToken: AccountToken
) : Asset

data class AssetError(
    override val chainId: Int,
    override val privateKey: String,
    val accountAddress: String,
    val tokenAddress: String,
    val error: Throwable
) : Asset