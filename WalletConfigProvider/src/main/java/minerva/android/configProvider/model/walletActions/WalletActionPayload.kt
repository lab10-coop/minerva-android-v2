package minerva.android.configProvider.model.walletActions

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class WalletActionPayload(
    @SerializedName("type")
    private val _type: Int? = Int.InvalidValue,
    @SerializedName("status")
    private val _status: Int? = Int.InvalidValue,
    @SerializedName("lastUsed")
    private val _lastUsed: Long? = Long.InvalidValue,
    @SerializedName("fields")
    private val _fields: HashMap<String, String>? = hashMapOf()
) {
    val type: Int get() = _type ?: Int.InvalidValue
    val status: Int get() = _status ?: Int.InvalidValue
    val lastUsed: Long get() = _lastUsed ?: Long.InvalidValue
    val fields: HashMap<String, String> get() = _fields ?: hashMapOf()
}