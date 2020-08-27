package minerva.android.walletmanager.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.mappers.*

data class Credential(
    @SerializedName(CREDENTIAL_NAME)
    override var name: String = String.Empty,
    override val type: String = String.Empty,
    val issuer: String = String.Empty,
    @SerializedName(NAME)
    val memberName: String = String.Empty,
    @SerializedName(MEMBER_ID)
    val memberId: String = String.Empty,
    @SerializedName(COVERAGE)
    val coverage: String = String.Empty,
    @SerializedName(EXP)
    val expirationDate: Long = Long.InvalidValue,
    @SerializedName(SINCE)
    val creationDate: String = String.Empty,
    val loggedInIdentityDid: String = String.Empty,
    override var lastUsed: Long = Long.InvalidValue,
    override var isDeleted: Boolean = false
) : MinervaPrimitive(name = name, type = type)