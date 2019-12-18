package minerva.android.configProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class ValueResponse(
    @SerializedName("index")
    private val _index: String?,
    @SerializedName("name")
    private val _name: String?,
    @SerializedName("network")
    private val _network: String?
) {
    val index: String
        get() = _index ?: String.Empty
    val name: String
        get() = _name ?: String.Empty
    val network: String
        get() = _network ?: String.Empty
}