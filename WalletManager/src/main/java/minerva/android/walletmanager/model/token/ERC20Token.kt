package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse

data class ERC20Token(
    override val chainId: Int,
    override val name: String = String.Empty,
    override val symbol: String = String.Empty,
    val address: String = String.Empty,
    val decimals: String = String.Empty,
    val logoURI: String? = null
) : Token {
    override fun equals(other: Any?): Boolean =
        (other as? ERC20Token)?.let {
            this.address == it.address
        }.orElse { false }
}