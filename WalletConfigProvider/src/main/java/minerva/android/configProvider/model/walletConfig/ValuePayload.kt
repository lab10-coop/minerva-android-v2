package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId

data class ValuePayload(
    @SerializedName("index")
    private val _index: Int?,
    @SerializedName("name")
    private val _name: String? = String.Empty,
    @SerializedName("network")
    private val _network: String? = String.Empty,
    @SerializedName("isDeleted")
    private val _isDeleted: Boolean? = false,
    @SerializedName("owners")
    private val _owners: MutableList<String>? = null,
    @SerializedName("smartContractAddress")
    private val _contractAddress: String? = null
) {
    val index: Int
        get() = _index ?: Int.InvalidId
    val name: String
        get() = _name ?: String.Empty
    val network: String
        get() = _network ?: String.Empty
    val isDeleted: Boolean
        get() = _isDeleted ?: false
    val owners: MutableList<String>?
        get() = _owners
    val contractAddress: String
        get() = _contractAddress ?: String.Empty

}