package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class Identity(
    val index: String,
    val publicKey: String = String.Empty,
    val privateKey: String = String.Empty,
    val identityName: String = String.Empty,
    val data: LinkedHashMap<String, String> = linkedMapOf(),
    val isRemovable: Boolean = true,
    val isDeleted: Boolean = false
)