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
    var accountAddress: String = String.Empty,
    var logoURI: String? = null,
    var tag: String = String.Empty
) : Token {

    override fun equals(other: Any?): Boolean =
        (other as? ERC20Token)?.let { address.equals(it.address, true) }.orElse { false }

    constructor(chainId: Int, tokenTx: TokenTx, accountAddress: String) : this(
        chainId,
        tokenTx.tokenName,
        tokenTx.tokenSymbol,
        tokenTx.address,
        tokenTx.tokenDecimal,
        accountAddress
    )
}