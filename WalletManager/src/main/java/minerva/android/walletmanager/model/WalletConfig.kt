package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.model.token.ERC20Token

data class WalletConfig(
    val version: Int = Int.InvalidId,
    val identities: List<Identity> = listOf(),
    val accounts: List<Account> = listOf(),
    val services: List<Service> = listOf(),
    val credentials: List<Credential> = listOf(),
    val erc20Tokens: Map<String, List<ERC20Token>> = mapOf()
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