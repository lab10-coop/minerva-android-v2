package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidId

data class WalletConfig(
    val version: Int = Int.InvalidId,
    val identities: List<Identity> = listOf(),
    val accounts: List<Account> = listOf(),
    val services: List<Service> = listOf(),
    val credentials: List<Credential> = listOf()
) {
    val newIndex: Int
        get() = identities.size + accounts.size

    val updateVersion: Int
        get() = version + 1

    val hasActiveAccount: Boolean
        get() = accounts.none { !it.isDeleted }
}