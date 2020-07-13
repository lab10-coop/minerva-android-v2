package minerva.android.walletmanager.utils

import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.model.defs.NetworkTokenName
import minerva.android.walletmanager.storage.ServiceType

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
            Account(2, "publicKey1", "privateKey1", "address", network = NetworkShortName.ETH_RIN),
            Account(4, "publicKey2", "privateKey2", "address", network = NetworkShortName.ATS_TAU),
            Account(
                5, "publicKey3", "privateKey3", "address", network = NetworkShortName.ATS_TAU,
                owners = listOf("masterOwner")
            ),
            Account(
                6, "publicKey4", "privateKey4", "address", network = NetworkShortName.ATS_TAU,
                owners = listOf("notMasterOwner", "masterOwner")
            ),
            Account(7, "publicKey5", "privateKey5", "address", network = NetworkShortName.ATS_TAU)
        )
        , listOf(
            Service(ServiceType.CHARGING_STATION, "name")
        )
    )
}