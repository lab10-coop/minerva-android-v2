package minerva.android.walletmanager.model.network

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.token.ERCToken
import java.math.BigInteger

data class Network(
    val name: String = String.Empty,
    val isActive: Boolean = true,
    val token: String = String.Empty,
    val httpRpc: String = String.Empty,
    val wsRpc: String = String.Empty,
    val isSafeAccountAvailable: Boolean = false,
    val gasPrice: BigInteger = BigInteger.TEN,
    val minGasPrice: BigInteger = BigInteger.TEN,
    val gasPriceOracle: String = String.Empty,
    val tokens: List<ERCToken> = emptyList(),
    val color: String = String.Empty,
    val testNet: Boolean = true,
    val chainId: Int = Int.InvalidValue,
    //base part of web path to explore transaction(web page address for getting info about specified transaction)
    val explore: String = String.Empty,
    val superfluid: SuperFluid? = null
) {
    fun isAvailable(): Boolean = httpRpc != String.Empty
}