package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import java.math.BigInteger

data class Network(
    val full: String = String.Empty,
    val short: String = String.Empty,
    val token: String = String.Empty,
    val httpRpc: String = String.Empty,
    val wsRpc: String = String.Empty,
    val isSafeAccountAvailable: Boolean = false,
    val gasPrice: BigInteger = BigInteger.TEN,
    val gasPriceOracle: String = String.Empty,
    val assets: List<Asset> = emptyList(),
    val color: String = String.Empty,
    val testNet: Boolean = true
) {
    fun isAvailable(): Boolean = httpRpc != String.Empty

    fun getAssetsAddresses(): List<String> = assets.map { it.address }
}