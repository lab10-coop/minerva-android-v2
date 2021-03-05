package minerva.android.blockchainprovider.utils

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow

private const val SLASH = "/"
private const val ENCODED_SLASH = "%2F"
private const val VALUE_NAME_PATTERN = "#%d %s"

object CryptoUtils {
    fun encodePublicKey(publicKey: String) = publicKey.replace(SLASH, ENCODED_SLASH)
    fun prepareName(networkName: String, index: Int) = String.format(VALUE_NAME_PATTERN, index + 1, networkName)
    fun convertTokenAmount(balance: BigDecimal, decimals: Int): BigInteger =
        balance.multiply(10.0.pow(decimals).toBigDecimal()).toBigInteger()
}