package minerva.android.walletmanager.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class Credential(
    @SerializedName("credentialName")
    override var name: String = String.Empty,
    override val type: String = String.Empty,
    val issuer: String = String.Empty,
    @SerializedName("name")
    val memberName: String = String.Empty,
    @SerializedName("memberId")
    val memberId: String = String.Empty,
    @SerializedName("coverage")
    val coverage: String = String.Empty,
    @SerializedName("exp")
    val expirationDate: Long = Long.InvalidValue,
    @SerializedName("since")
    val creationDate: String = String.Empty,
    val loggedInIdentityDid: String = String.Empty,
    var lastUsed: Long = Long.InvalidValue
) : MinervaPrimitive(name = name, type = type)