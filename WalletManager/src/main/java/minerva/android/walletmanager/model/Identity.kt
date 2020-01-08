package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class Identity(
    val index: Int,
    val name: String = String.Empty,
    val publicKey: String = String.Empty,
    val privateKey: String = String.Empty,
    val data: LinkedHashMap<String, String> = linkedMapOf(),
    val isDeleted: Boolean = false
) {
    constructor(index: Int, identity: Identity) : this(
        index,
        identity.name,
        identity.publicKey,
        identity.privateKey,
        identity.data,
        identity.isDeleted
    )
}