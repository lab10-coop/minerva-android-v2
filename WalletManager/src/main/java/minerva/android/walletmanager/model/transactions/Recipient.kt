package minerva.android.walletmanager.model.transactions

import minerva.android.kotlinUtils.Empty

data class Recipient(
    val ensName: String = String.Empty,
    val address: String = String.Empty
) {
    fun getData(): String = if (ensName != String.Empty) ensName else address
}