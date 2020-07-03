package minerva.android.walletmanager.utils

import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.NetworkShortName

object DataProvider {

    val data = linkedMapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    )

    val walletConfig = WalletConfig(
        0, listOf(
            Identity(0, "identityName1", "", "privateKey", data),
            Identity(1, "identityName2", "", "privateKey", data),
            Identity(3, "identityName3", "", "privateKey", data)
        ),
        listOf(
            Account(2, "publicKey1", "privateKey1", "address", network = NetworkShortName.ETH),
            Account(4, "publicKey2", "privateKey2", "address", network = NetworkShortName.ATS),
            Account(
                5, "publicKey3", "privateKey3", "address", network = NetworkShortName.ATS,
                owners = listOf("masterOwner")
            ),
            Account(
                6, "publicKey4", "privateKey4", "address", network = NetworkShortName.ATS,
                owners = listOf("notMasterOwner", "masterOwner")
            ),
            Account(7, "publicKey5", "privateKey5", "address", network = NetworkShortName.ATS)
        )
    )

    val walletConfig2 = WalletConfig(
        0, listOf(),
        listOf(
            Account(2, "publicKey11", "privateKey1", "address", network = NetworkShortName.ETH),
            Account(4, "publicKey22", "privateKey2", "address", network = NetworkShortName.ETH),
            Account(
                5, "publicKey33", "privateKey3", "masterOwner", network = NetworkShortName.ETH,
                owners = listOf("masterOwner")
            ),
            Account(
                6, "publicKey44", "privateKey4", "address", network = NetworkShortName.ETH,
                owners = listOf("notMasterOwner", "masterOwner")
            )
        )
    )
}