package minerva.android.walletmanager.model.minervaprimitives.credential

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.mappers.*
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive

data class Credential(
    @SerializedName(CREDENTIAL_NAME)
    override var name: String = String.Empty,
    val type: String = String.Empty,
    val membershipType: String = String.Empty,
    val issuer: String = String.Empty,
    val token: String = String.Empty,
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
    val cardUrl: String? = String.Empty,
    override val iconUrl: String? = String.Empty
) : MinervaPrimitive(name = name, lastUsed = lastUsed)