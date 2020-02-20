package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidId

data class WalletActionConfig(
    val version: Int = Int.InvalidId,
    val actions: List<HashMap<Long, List<WalletAction>>> = listOf()
)