package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class ServicePayload(
    @SerializedName("type")
    private val _type: Int? = Int.InvalidValue,
    @SerializedName("name")
    private val _name: String? = String.Empty,
    @SerializedName("lastUsed")
    private val _lastUsed: String? = String.Empty
) {
    val type: Int
        get() = _type ?: Int.InvalidValue
    val name: String
        get() = _name ?: String.Empty
    val lastUsed: String
        get() = _lastUsed ?: String.Empty
}