package minerva.android.configProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidId

data class WalletConfigPayload(
    @SerializedName("version")
    private var _version: Int? = Int.InvalidId,
    @SerializedName("identities")
    private var _identityPayloads: List<IdentityPayload>? = listOf(),
    @SerializedName("values")
    private var _valuePayloads: List<ValuePayload>? = listOf(),
    @SerializedName("services")
    private var _servicesPayloads: List<ServicePayload>? = listOf()
) {
    val version: Int
        get() = _version ?: Int.InvalidId
    val identityResponse: List<IdentityPayload>
        get() = _identityPayloads ?: listOf()
    val valueResponse: List<ValuePayload>
        get() = _valuePayloads ?: listOf()
    val serviceResponse: List<ServicePayload>
        get() = _servicesPayloads ?: listOf()

    fun getIdentityPayload(index: Int): IdentityPayload {
        identityResponse.forEach {
            if(index == it.index) {
                return it
            }
        }
        return IdentityPayload(index)
    }

    fun getValuePayload(index: Int): ValuePayload {
        valueResponse.forEach {
            if(index == it.index) {
                return it
            }
        }
        return ValuePayload(index)
    }
}
