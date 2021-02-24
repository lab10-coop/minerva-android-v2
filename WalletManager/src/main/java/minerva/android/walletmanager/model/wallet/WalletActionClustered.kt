package minerva.android.walletmanager.model.wallet

import minerva.android.kotlinUtils.InvalidValue

data class WalletActionClustered(
    val lastUsed: Long = Long.InvalidValue,
    val walletActions: List<WalletAction> = listOf()
)