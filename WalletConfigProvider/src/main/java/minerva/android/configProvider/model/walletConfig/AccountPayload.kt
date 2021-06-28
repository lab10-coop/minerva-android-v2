package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidValue

data class AccountPayload(
    @SerializedName("index")
    private val _index: Int?,
    @SerializedName("name")
    private val _name: String? = String.Empty,
    @SerializedName("network")
    private val _chainId: Int? = Int.InvalidValue,
    @SerializedName("isDeleted")
    private val _isDeleted: Boolean? = false,
    @SerializedName("owners")
    private val _owners: List<String>? = null,
    @SerializedName("smartContractAddress")
    private val _contractAddress: String? = null,
    @SerializedName("bindedOwner")
    private val _bindedOwner: String? = String.Empty,
    @SerializedName("isTestNetwork")
    private val _isTestNetwork: Boolean? = false,
    @SerializedName("isHide")
    private val _isHide: Boolean? = false
) {
    val index: Int
        get() = _index ?: Int.InvalidId
    val name: String
        get() = _name ?: String.Empty
    val chainId: Int
        get() = _chainId ?: Int.InvalidValue
    val isDeleted: Boolean
        get() = _isDeleted ?: false
    val owners: List<String>?
        get() = _owners
    val contractAddress: String
        get() = _contractAddress ?: String.Empty
    val bindedOwner: String
        get() = _bindedOwner ?: String.Empty
    val isTestNetwork: Boolean
        get() = _isTestNetwork ?: false
    val isHide: Boolean
        get() = _isHide ?: false
}