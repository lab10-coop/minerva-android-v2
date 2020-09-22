package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import java.math.BigInteger

data class Network(
    val full: String = String.Empty,
    val short: String = String.Empty,
    val token: String = String.Empty,
    val https: String = String.Empty,
    val wss: String = String.Empty,
    val isSafeAccountAvailable: Boolean = false,
    val gasPrice: BigInteger = BigInteger.TEN,
    val assets: List<Asset> = emptyList(),
    val color: String = String.Empty
)