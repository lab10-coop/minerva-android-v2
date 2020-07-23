package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidValue

data class WalletAction(
    val type: Int = Int.InvalidValue,
    val status: Int = Int.InvalidValue,
    val lastUsed: Long = Long.InvalidValue,
    val fields: HashMap<String, String> = hashMapOf()
)