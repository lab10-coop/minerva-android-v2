package minerva.android.configProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidId

data class WalletConfigPayload(
    @SerializedName("version")
    private var _version: Int? = Int.InvalidId,
    @SerializedName("identities")
    private var _identityPayloads: List<IdentityPayload>? = listOf(),
    @SerializedName("values")
    private var _valuePayloads: List<ValuePayload>? = listOf()
) {
    val version: Int
        get() = _version ?: Int.InvalidId
    val identityResponses: List<IdentityPayload>
        get() = _identityPayloads ?: listOf()
    val valueResponses: List<ValuePayload>
        get() = _valuePayloads ?: listOf()
}
