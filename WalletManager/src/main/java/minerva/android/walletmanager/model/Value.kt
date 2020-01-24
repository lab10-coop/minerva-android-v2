package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import java.io.Serializable
import java.math.BigInteger

data class Value(
    val index: Int,
    var publicKey: String = String.Empty,
    var privateKey: String = String.Empty,
    val name: String = String.Empty,
    val network: String = String.Empty,
    val isDeleted: Boolean = false,
    var balance: BigInteger = Int.InvalidId.toBigInteger()
): Serializable