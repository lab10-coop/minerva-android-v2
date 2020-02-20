package minerva.android.walletmanager.model

import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.ServicePayload
import minerva.android.configProvider.model.walletConfig.ValuePayload

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

    val identityResponses = listOf(
        IdentityPayload(2, "IdentityName1", identityData, false),
        IdentityPayload(3, "IdentityName2", identityData, true)
    )

    val values = listOf(
        Value(1, "publicKey", "privateKey", "address", "ValuePayload1", Network.ARTIS.short),
        Value(2, "publicKey", "privateKey", "address", "ValuePayload2", Network.ETHEREUM.short)
    )

    val valuesResponse = listOf(
        ValuePayload(1, "ValuePayload1", Network.ARTIS.short, false),
        ValuePayload(2, "ValuePayload2", Network.ETHEREUM.short, false)
    )

    val services = listOf(
        Service(0, "Service1", "123"),
        Service(1, "Service2", "123")
    )

    val serviceResponse = listOf(
        ServicePayload(0, "Service1", "123"),
        ServicePayload(1, "Service2", "123")
    )
}
