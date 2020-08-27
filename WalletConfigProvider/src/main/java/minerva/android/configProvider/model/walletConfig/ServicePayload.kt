package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class ServicePayload(
    @SerializedName("type")
    private val _type: String? = String.Empty,
    @SerializedName("name")
    private val _name: String? = String.Empty,
    @SerializedName("lastUsed")
    private val _lastUsed: Long? = Long.InvalidValue,
    @SerializedName("loggedInIdentityPublicKey")
    private val _loggedInIdentityPublicKey: String? = String.Empty
) {
    val type: String
        get() = _type ?: String.Empty
    val name: String
        get() = _name ?: String.Empty
    val lastUsed: Long
        get() = _lastUsed ?: Long.InvalidValue
    val loggedInIdentityPublicKey: String
        get() = _loggedInIdentityPublicKey ?: String.Empty
}