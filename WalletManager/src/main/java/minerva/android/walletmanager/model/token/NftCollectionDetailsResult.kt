package minerva.android.walletmanager.model.token

import minerva.android.kotlinUtils.Empty

data class NftCollectionDetailsResult(
    val logoURI: String,
    val name: String = String.Empty,
    val symbol: String = String.Empty,
    val override: Boolean = false
)
