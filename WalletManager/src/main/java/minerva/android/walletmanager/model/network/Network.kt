package minerva.android.walletmanager.model.network

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.token.ERC20Token
import java.math.BigInteger

data class Network(
    val name: String = String.Empty,
    val token: String = String.Empty,
    val httpRpc: String = String.Empty,
    val wsRpc: String = String.Empty,
    val isSafeAccountAvailable: Boolean = false,
    val gasPrice: BigInteger = BigInteger.TEN,
    val gasPriceOracle: String = String.Empty,
    val tokens: List<ERC20Token> = emptyList(),
    val color: String = String.Empty,
    val testNet: Boolean = true,
    val chainId: Int = Int.InvalidValue,
    val superfluid: SuperFluid? = null
) {
    fun isAvailable(): Boolean = httpRpc != String.Empty
}