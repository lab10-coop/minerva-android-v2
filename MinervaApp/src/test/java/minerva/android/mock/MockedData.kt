package minerva.android.mock

import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network

val accounts = listOf(
    Account(1, chainId = 2, address = "account0"),
    Account(2, chainId = 1, address = "account1"),
    Account(3, chainId = 1, address = "account2"),
    Account(4, chainId = 3, address = "address3")
)

val networks = listOf(
    Network(chainId = 1),
    Network(chainId = 2),
    Network(chainId = 3)
)
