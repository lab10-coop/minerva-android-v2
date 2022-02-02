package minerva.android.accounts.nft.model

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import java.math.BigDecimal


data class NftItem(
    val tokenAddress: String = String.Empty,
    val tokenId: String = String.Empty,
    val description: String = String.Empty,
    val contentUrl: String = String.Empty,
    val name: String = String.Empty,
    val isERC1155: Boolean = false,
    val decimals: String = String.Empty,
    var balance: BigDecimal = BigDecimal.ZERO,
    var isDescriptionExpanded: Boolean = false,
    var wasSent: Boolean = false
) {
    companion object {
        val Invalid = NftItem()
    }
}
