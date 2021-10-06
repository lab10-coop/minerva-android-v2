package minerva.android.walletmanager.model.minervaprimitives.account

import minerva.android.walletmanager.model.transactions.Balance
import java.math.BigDecimal

interface Coin {
    val chainId: Int
    val address: String
}

data class CoinBalance(
    override val chainId: Int,
    override val address: String,
    val balance: Balance,
    val rate: Double? = null
) : Coin

data class CoinError(
    override val chainId: Int,
    override val address: String,
    val error: Throwable
) : Coin

data class CoinCryptoBalance(
    override val chainId: Int,
    override val address: String,
    val balance: BigDecimal
): Coin