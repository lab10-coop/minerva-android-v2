package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidId

data class WalletConfig(
    val version: Int = Int.InvalidId,
    val identities: List<Identity> = listOf(),
    val values: List<Value> = listOf()
) {
    val newIndex: Int
        get() = identities.size + values.size
}