package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class Value(
    val index: String,
    val publicKey: String = String.Empty,
    val privateKey: String = String.Empty,
    val name: String = String.Empty,
    val isDeleted: Boolean = false
)