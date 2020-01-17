package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.InvalidVersion

data class Service(
    val type: Int = Int.InvalidVersion,
    val name: String = String.Empty,
    val lastUsed: String = String.Empty
)