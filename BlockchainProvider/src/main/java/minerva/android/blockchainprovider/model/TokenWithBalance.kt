package minerva.android.blockchainprovider.model

import java.math.BigDecimal

interface Token {
    val chainId: Int
    val address: String
    val tokenId: String?
}

data class TokenWithBalance(
    override val chainId: Int,
    override val address: String,
    val balance: BigDecimal,
    override val tokenId: String? = null
) : Token

data class TokenWithError(
    override val chainId: Int,
    override val address: String,
    val error: Throwable,
    override val tokenId: String? = null
) : Token