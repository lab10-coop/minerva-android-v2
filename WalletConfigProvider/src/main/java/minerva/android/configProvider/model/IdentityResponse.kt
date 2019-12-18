package minerva.android.configProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class IdentityResponse(
    @SerializedName("index")
    private val _index: String?,
    @SerializedName("name")
    private val _name: String?,
    @SerializedName("data")
    private val _data: LinkedHashMap<String, String>?
) {
    val index: String
        get() = _index ?: String.Empty
    val name: String
        get() = _name ?: String.Empty
    val data: LinkedHashMap<String, String>
        get() = _data ?: linkedMapOf()
}