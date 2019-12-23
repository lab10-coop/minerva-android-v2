package minerva.android.walletmanager.model

import minerva.android.configProvider.model.IdentityPayload
import minerva.android.configProvider.model.ValuePayload


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

    val identities = listOf(
        Identity(2, "pubicKey", "privateKey", "IdentityName1", identityData, false),
        Identity(3, "pubicKey", "privateKey", "IdentityName2", identityData, true)
    )

    val identityResponses = listOf(
        IdentityPayload(2, "IdentityName1", identityData, false),
        IdentityPayload(3, "IdentityName2", identityData, true)
    )

    val values = listOf(
        Value(0, "", "", "Value 1", "Network 1"),
        Value(1, "", "", "Value 2", "Network 2")
    )

    val valuesResponse = listOf(
        ValuePayload(0, "Value 1", ""),
        ValuePayload(1, "Value 2", "")
    )
}
