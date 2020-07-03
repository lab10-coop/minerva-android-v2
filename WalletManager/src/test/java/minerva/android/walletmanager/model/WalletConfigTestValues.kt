package minerva.android.walletmanager.model

import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.AccountPayload

open class WalletConfigTestValues {

    val identityData: LinkedHashMap<String, String> = linkedMapOf(
        "Name" to "Tom Johnson",
        "Email" to "tj@mail.com",
        "Date of Brith" to "12.09.1991",
        "Some Key" to "Some value",
        "Some Key 2" to "Some value",
        "Some Key 3" to "Some value",
        "Some Key 4" to "Some value"
    )

    val identity = listOf(
        Identity(0, "IdentityName1", "publicKey", "privateKey", identityData, false)
    )

    val onlineIdentity = listOf(
        Identity(0, "OnlineIdentityName1", "publicKey", "privateKey", identityData, false)
    )

    val identityResponse = listOf(
        IdentityPayload(0, "IdentityName1", identityData, false)
    )

    val onlineIdentityResponse = listOf(
        IdentityPayload(0, "OnlineIdentityName1", identityData, false)
    )

    val identities = listOf(
        Identity(2, "pubicKey", "privateKey", "IdentityName1", identityData, false),
        Identity(3, "pubicKey", "privateKey", "IdentityName2", identityData, true)
    )

    val values = listOf(
        Account(1, "publicKey", "privateKey", "address", "ValuePayload1", Network.ARTIS.short),
        Account(2, "publicKey", "privateKey", "address", "ValuePayload2", Network.ETHEREUM.short)
    )

    val valuesResponse = listOf(
        AccountPayload(1, "ValuePayload1", Network.ARTIS.short, false),
        AccountPayload(2, "ValuePayload2", Network.ETHEREUM.short, false)
    )
}
