package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

//TODO complete model with correct fields from qr code
data class CredentialsPayload(
    @SerializedName("name")
    private val _name: String? = String.Empty,
    @SerializedName("type")
    private val _type: String? = String.Empty,
    @SerializedName("lastUsed")
    private val _lastUsed: String? = String.Empty
) {
    val name: String
        get() = _name ?: String.Empty
    val type: String
        get() = _type ?: String.Empty
    val lastUsed: String
        get() = _lastUsed ?: String.Empty
}