package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.InvalidValue

data class WalletConfigPayload(
    @SerializedName("modelVersion")
    var modelVersion: Double? = Double.InvalidValue,
    @SerializedName("version")
    private var _version: Int? = Int.InvalidId,
    @SerializedName("identities")
    private var _identityPayloads: List<IdentityPayload>? = listOf(),
    @SerializedName("accounts")
    private var _accountPayloads: List<AccountPayload>? = listOf(),
    @SerializedName("services")
    private var _servicesPayloads: List<ServicePayload>? = listOf(),
    @SerializedName("credentials")
    private var _credentialPayloads: List<CredentialsPayload>? = listOf(),
    @SerializedName("ERC20Tokens")
    private var _erc20Tokens: Map<String, List<TokenPayload>>? = mapOf() //TODO it will be splitted with Swarm implementation
) {
    val version: Int
        get() = _version ?: Int.InvalidId
    val identityResponse: List<IdentityPayload>
        get() = _identityPayloads ?: listOf()
    val accountResponse: List<AccountPayload>
        get() = _accountPayloads ?: listOf()
    val serviceResponse: List<ServicePayload>
        get() = _servicesPayloads ?: listOf()
    val credentialResponse: List<CredentialsPayload>
        get() = _credentialPayloads ?: listOf()
    val erc20TokenResponse: Map<String, List<TokenPayload>>
        get() = _erc20Tokens ?: mapOf()
}
