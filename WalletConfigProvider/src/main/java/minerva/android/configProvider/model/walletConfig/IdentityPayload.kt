package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId

data class IdentityPayload(
    @SerializedName("index")
    private val _index: Int?,
    @SerializedName("name")
    private val _name: String? = String.Empty,
    @SerializedName("data")
    private val _data: LinkedHashMap<String, String>? = linkedMapOf(),
    @SerializedName("isDeleted")
    private val _isDeleted: Boolean? = false
) {
    val index: Int
        get() = _index ?: Int.InvalidId
    val name: String
        get() = _name ?: String.Empty
    val data: LinkedHashMap<String, String>
        get() = _data ?: linkedMapOf()
    val isDeleted: Boolean
        get() = _isDeleted ?: false
}
