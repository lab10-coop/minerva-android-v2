package minerva.android.walletmanager.model

data class Identity(
    val index: Int,
    val publicKey: String,
    val privateKey: String,
    val identityName: String,
    val data:LinkedHashMap<String, String> = linkedMapOf(),
    val removable: Boolean = true
)