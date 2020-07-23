package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class Asset (
    val name: String = String.Empty,
    val nameShort: String = String.Empty,
    val address: String = String.Empty
)