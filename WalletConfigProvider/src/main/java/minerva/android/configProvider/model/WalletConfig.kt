package minerva.android.configProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class WalletConfig(
    @SerializedName("version")
    private var _version: String? = String.Empty,
    @SerializedName("identities")
    private var _identities: List<IdentityResponse>? = listOf(),
    @SerializedName("values")
    private var _values: List<ValueResponse>? = listOf()
) {
    val version: String
        get() = _version ?: String.Empty
    val identities: List<IdentityResponse>
        get() = _identities ?: listOf()
    val values: List<ValueResponse>
        get() = _values ?: listOf()
}
