package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import java.math.BigInteger
import minerva.android.kotlinUtils.InvalidId

data class Value(
    val index: Int,
    val publicKey: String = String.Empty,
    val privateKey: String = String.Empty,
    val name: String = String.Empty,
    val network: String = String.Empty,
    val isDeleted: Boolean = false,
    var balance: BigInteger = Int.InvalidId.toBigInteger()
)