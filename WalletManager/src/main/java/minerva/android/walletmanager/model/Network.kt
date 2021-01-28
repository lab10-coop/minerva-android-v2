package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.token.ERC20Token
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
    val tokens: List<ERC20Token> = emptyList(),
    val color: String = String.Empty,
    val testNet: Boolean = true,
    val chainId: Int = Int.InvalidValue
) {
    fun isAvailable(): Boolean = httpRpc != String.Empty

    fun getTokensAddresses(): List<String> = tokens.map { it.address }
}