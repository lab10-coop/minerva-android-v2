package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidId

data class WalletConfigPayload(
    @SerializedName("version")
    private var _version: Int? = Int.InvalidId,
    @SerializedName("identities")
    private var _identityPayloads: List<IdentityPayload>? = listOf(),
    @SerializedName("accounts")
    private var _accountPayloads: List<AccountPayload>? = listOf(),
    @SerializedName("services")
    private var _servicesPayloads: List<ServicePayload>? = listOf()
) {
    val version: Int
        get() = _version ?: Int.InvalidId
    val identityResponse: List<IdentityPayload>
        get() = _identityPayloads ?: listOf()
    val accountResponse: List<AccountPayload>
        get() = _accountPayloads ?: listOf()
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

    fun getAccountPayload(index: Int): AccountPayload {
        accountResponse.forEach {
            if(index == it.index) {
                return it
            }
        }
        return AccountPayload(index)
    }
}
