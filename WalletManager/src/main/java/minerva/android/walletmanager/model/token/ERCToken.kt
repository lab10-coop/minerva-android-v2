package minerva.android.walletmanager.model.token

import androidx.room.Entity
import androidx.room.PrimaryKey
import minerva.android.apiProvider.model.TokenTx
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse
import java.math.BigInteger

@Entity(tableName = "tokens")
data class ERCToken(
    override val chainId: Int,
    override var name: String = String.Empty,
    override val symbol: String = String.Empty,
    @PrimaryKey val address: String = String.Empty,
    val decimals: String = String.Empty,
    var accountAddress: String = String.Empty,
    var tokenId: String? = null,
    val type: TokenType,
    var logoURI: String? = null,
    var tag: String = String.Empty,
    var isError: Boolean = false,
    var isStreamActive: Boolean = false,
    var contentUri: String = String.Empty,
    var description: String = String.Empty,
    var consNetFlow: BigInteger = BigInteger.ZERO,
    var collectionName: String? = null
) : Token {

    override fun equals(other: Any?): Boolean =
        (other as? ERCToken)?.let { address.equals(it.address, true) }.orElse { false }

    constructor(chainId: Int, tokenTx: TokenTx, accountAddress: String, tokenType: TokenType) : this(
        chainId,
        if (tokenType.isERC20()) tokenTx.tokenName else String.Empty,
        tokenTx.tokenSymbol,
        tokenTx.address,
        tokenTx.tokenDecimal,
        accountAddress,
        tokenTx.tokenId,
        tokenType,
        collectionName = if (tokenType.isERC721()) tokenTx.tokenName else null
    )
}

enum class TokenType {
    ERC20, ERC721, SUPER_TOKEN, WRAPPER_TOKEN, INVALID;

    fun isERC721() = this == ERC721
    fun isERC20() = when (this) {
        ERC721 -> false
        else -> true
    }
}