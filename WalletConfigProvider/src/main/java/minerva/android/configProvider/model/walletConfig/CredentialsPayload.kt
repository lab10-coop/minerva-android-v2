package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class CredentialsPayload(
    @SerializedName("name")
    private val _name: String? = String.Empty,
    @SerializedName("type")
    private val _type: String? = String.Empty,
    @SerializedName("issuer")
    private val _issuer: String? = String.Empty,
    @SerializedName("memberName")
    private val _memberName: String? = String.Empty,
    @SerializedName("memberId")
    private val _memberId: String? = String.Empty,
    @SerializedName("coverage")
    private val _coverage: String? = String.Empty,
    @SerializedName("expirationDate")
    private val _expirationDate: Long? = Long.InvalidValue,
    @SerializedName("creationDate")
    private val _creationDate: String? = String.Empty,
    @SerializedName("loggedInIdentityDid")
    private val _loggedInIdentityDid: String? = String.Empty,
    @SerializedName("lastUsed")
    private val _lastUsed: String? = String.Empty
) {
    val name: String
        get() = _name ?: String.Empty
    val type: String
        get() = _type ?: String.Empty
    val issuer: String
        get() = _issuer ?: String.Empty
    val memberName: String
        get() = _memberName ?: String.Empty
    val memberId: String
        get() = _memberId ?: String.Empty
    val coverage: String
        get() = _coverage ?: String.Empty
    val expirationDate: Long
        get() = _expirationDate ?: Long.InvalidValue
    val creationDate: String
        get() = _creationDate ?: String.Empty
    val loggedInIdentityDid: String
        get() = _loggedInIdentityDid ?: String.Empty
    val lastUsed: String
        get() = _lastUsed ?: String.Empty
}