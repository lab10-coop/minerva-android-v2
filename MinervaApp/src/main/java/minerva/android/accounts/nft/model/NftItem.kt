package minerva.android.accounts.nft.model

import java.math.BigDecimal


data class NftItem(
    val tokenAddress: String,
    val tokenId: String,
    val description: String,
    val contentUrl: String,
    val name: String,
    val isERC1155: Boolean,
    val balance: BigDecimal = BigDecimal.ZERO,
    var isDescriptionExpanded: Boolean = false
)
