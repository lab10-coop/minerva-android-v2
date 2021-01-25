package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.function.orElse

data class Token(
    val name: String = String.Empty,
    val symbol: String = String.Empty,
    val address: String = String.Empty,
    val decimals: String = String.Empty,
    val logoRes: Int = Int.InvalidValue
) {
    override fun equals(other: Any?): Boolean =
        (other as? Token)?.let {
            this.address == it.address
        }.orElse { false }
}