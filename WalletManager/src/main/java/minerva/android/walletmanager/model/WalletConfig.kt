package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty

data class WalletConfig(
    val version: String = String.Empty,
    val identities: List<Identity> = listOf(),
    val values: List<Value> = listOf()
) {
    val newIndex: Int
        get() = identities.size + values.size
}