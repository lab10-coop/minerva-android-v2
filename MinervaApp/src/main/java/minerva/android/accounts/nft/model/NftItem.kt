package minerva.android.accounts.nft.model

data class NftItem(
    val tokenAddress: String,
    val tokenId: String,
    val description: String,
    val contentUrl: String,
    val name: String
)
