package minerva.android.walletmanager.model.minervaprimitives.account

import minerva.android.walletmanager.model.transactions.Balance

data class CoinBalance(
    val chainId: Int,
    val address: String,
    val balance: Balance
)
