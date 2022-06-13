package minerva.android.accounts.nft.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.ContentType
import minerva.android.walletmanager.model.NftContent
import java.math.BigDecimal


data class NftItem(
    val tokenAddress: String = String.Empty,
    val tokenId: String = String.Empty,
    val nftContent: NftContent = NftContent(),
    val name: String = String.Empty,
    val isERC1155: Boolean = false,
    val decimals: String = String.Empty,
    var balance: BigDecimal = BigDecimal.ZERO,
    var isDescriptionExpanded: Boolean = false,
    var wasSent: Boolean = false,
    val isFavorite: Boolean = false //isFavorite - used for nft status
) {
    companion object {
        val Invalid = NftItem()
    }
}
