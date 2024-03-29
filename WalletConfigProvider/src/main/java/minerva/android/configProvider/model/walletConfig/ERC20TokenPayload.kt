package minerva.android.configProvider.model.walletConfig

import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class ERC20TokenPayload(
    val chainId: Int = Int.InvalidValue,
    val name: String = String.Empty,
    val symbol: String = String.Empty,
    val address: String = String.Empty,
    val decimals: String = String.Empty,
    val logoURI: String? = null,
    val accountAddress: String = String.Empty,
    val tag: String = String.Empty,
    val type: String = String.Empty,
    val tokenId: String? = null,
    val collectionName: String? = null,
    val nftContentPayload: NftContentPayload? = null,
    val isFavorite: Boolean = false //isFavorite - used for nft status
)