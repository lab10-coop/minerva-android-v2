package minerva.android.configProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class WalletConfigPayload(
    @SerializedName("version")
    private var _version: String?,
    @SerializedName("identities")
    private var _identities: List<IdentityPayload>? = listOf(),
    @SerializedName("values")
    private var _values: List<ValuePayload>? = listOf()
) {
    val version: String
        get() = _version ?: String.Empty
    val identities: List<IdentityPayload>
        get() = _identities ?: listOf()
    val values: List<ValuePayload>
        get() = _values ?: listOf()
}
