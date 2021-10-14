package minerva.android.walletmanager.model.wallet

import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.credential.Credential
import minerva.android.walletmanager.model.token.ERCToken

data class WalletConfig(
    val version: Int = Int.InvalidId,
    val identities: List<Identity> = listOf(),
    val accounts: List<Account> = listOf(),
    val services: List<Service> = listOf(),
    val credentials: List<Credential> = listOf(),
    val erc20Tokens: Map<Int, List<ERCToken>> = mapOf()
) {
    val updateVersion: Int
        get() = version + 1

    val newIdentityIndex: Int
        get() = identities.size

    val newTestNetworkIndex: Int
        get() = accounts.filter { it.network.testNet }.size

    val newMainNetworkIndex: Int
        get() = accounts.filter { !it.network.testNet }.size

    val hasActiveAccount: Boolean
        get() = accounts.none { !it.isDeleted }
}