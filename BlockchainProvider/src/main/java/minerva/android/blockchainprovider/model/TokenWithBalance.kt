package minerva.android.blockchainprovider.model

import java.math.BigDecimal

interface Token {
    val chainId: Int
    val address: String
}

data class TokenWithBalance(
    override val chainId: Int,
    override val address: String,
    val balance: BigDecimal
) : Token

data class TokenWithError(
    override val chainId: Int,
    override val address: String,
    val error: Throwable
) : Token