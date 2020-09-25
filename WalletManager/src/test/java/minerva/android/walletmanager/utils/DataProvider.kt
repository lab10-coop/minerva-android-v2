package minerva.android.walletmanager.utils

import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.NetworkShortName
import minerva.android.walletmanager.model.defs.ServiceType

object DataProvider {

    val data = linkedMapOf(
        "key1" to "value1",
        "key2" to "value2",
        "key3" to "value3"
    )

    val walletConfig = WalletConfig(
        0, listOf(
            Identity(0, "identityName1", "11", "privateKey", "address1", data),
            Identity(1, "identityName2", "", "privateKey", "address2", data),
            Identity(3, "identityName3", "", "privateKey", "address3", data)
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
        ),
        listOf(Credential(loggedInIdentityDid = "did:ethr:address", type = "type", issuer = "iss"))
    )
}