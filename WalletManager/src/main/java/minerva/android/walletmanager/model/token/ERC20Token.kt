package minerva.android.walletmanager.model.token

import androidx.room.Entity
import androidx.room.PrimaryKey
import minerva.android.apiProvider.model.TokenTx
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse

@Entity(tableName = "tokens")
data class ERC20Token(
    override val chainId: Int,
    override val name: String = String.Empty,
    override val symbol: String = String.Empty,
    @PrimaryKey val address: String = String.Empty,
    //TODO change to int
    val decimals: String = String.Empty,
    var logoURI: String? = null,
    val tag: String = String.Empty
) : Token {

    override fun equals(other: Any?): Boolean =
        (other as? ERC20Token)?.let {
            address.equals(it.address, true)
        }.orElse { false }

//    override fun hashCode(): Int {
//        var result = chainId
//        result = 31 * result + name.hashCode()
//        result = 31 * result + symbol.hashCode()
//        result = 31 * result + address.hashCode()
//        result = 31 * result + decimals.hashCode()
//        result = 31 * result + (logoURI?.hashCode() ?: 0)
//        result = 31 * result + tag.hashCode()
//        return result
//    }

    constructor(chainId: Int, tokenTx: TokenTx) : this(
        chainId,
        tokenTx.tokenName,
        tokenTx.tokenSymbol,
        tokenTx.address,
        tokenTx.tokenDecimal
    )
}