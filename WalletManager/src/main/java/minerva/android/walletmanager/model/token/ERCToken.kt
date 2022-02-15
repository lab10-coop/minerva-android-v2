package minerva.android.walletmanager.model.token

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import minerva.android.apiProvider.model.TokenTx
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.model.ContentType
import minerva.android.walletmanager.model.NftContent
import java.math.BigInteger

@Entity(tableName = "tokens")
data class ERCToken(
    override val chainId: Int,
    override var name: String = String.Empty,
    override var symbol: String = String.Empty,
    @PrimaryKey val address: String = String.Empty,
    val decimals: String = String.Empty,
    var accountAddress: String = String.Empty,
    var tokenId: String? = null,
    val type: TokenType,
    var logoURI: String? = null,
    var tag: String = String.Empty,
    var isError: Boolean = false,
    var isStreamActive: Boolean = false,
    @Embedded var nftContent: NftContent = NftContent(),
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
        collectionName = if (tokenType.isNft()) tokenTx.tokenName else null
    )

    fun mergeNftDetailsAfterTokenDiscovery(ercToken: ERCToken){
        mergePropertiesWithLocalFirstStrategy(ercToken)
        mergePropertiesWithRemoteFirstStrategy(ercToken)
    }

    private fun mergePropertiesWithLocalFirstStrategy(ercToken: ERCToken){
        logoURI = ercToken.logoURI
        collectionName = ercToken.collectionName
        symbol = ercToken.symbol
        name = ercToken.name
    }

    private fun mergePropertiesWithRemoteFirstStrategy(ercToken: ERCToken){
        if(nftContent.imageUri.isEmpty()) nftContent.imageUri = ercToken.nftContent.imageUri
        if(nftContent.contentType == ContentType.INVALID) nftContent.contentType = ercToken.nftContent.contentType
        if(nftContent.animationUri.isEmpty()) nftContent.animationUri = ercToken.nftContent.animationUri
        if(nftContent.tokenUri.isEmpty()) nftContent.tokenUri = ercToken.nftContent.tokenUri
        if(description.isEmpty()) description = ercToken.description
    }
}

enum class TokenType {
    ERC20, ERC721, SUPER_TOKEN, WRAPPER_TOKEN, INVALID, ERC1155;

    fun isERC1155() = this == ERC1155
    fun isERC721() = this == ERC721
    fun isERC20() = when (this) {
        ERC721, ERC1155 -> false
        else -> true
    }

    fun isNft() = isERC1155() || isERC721()
}