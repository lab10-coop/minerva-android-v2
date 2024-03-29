package minerva.android.walletmanager.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.walletmanager.model.defs.CredentialType

@Parcelize
open class QrCode(
    open var issuer: String = String.Empty,
    open var token: String = String.Empty,
    open val iconUrl: String? = String.Empty
) : Parcelable

@Parcelize
data class ServiceQrCode(
    override var issuer: String = String.Empty,
    var serviceName: String = String.Empty,
    var callback: String? = String.Empty,
    var requestedData: List<String> = listOf(),
    var identityFields: String = String.Empty,
    override val iconUrl: String? = String.Empty
) : QrCode(issuer = issuer)

@Parcelize
data class CredentialQrCode(
    override var issuer: String = String.Empty,
    override var token: String = String.Empty,
    val name: String = String.Empty,
    val membershipType: CredentialType = CredentialType.DEFAULT,
    val type: CredentialType = CredentialType.DEFAULT,
    val memberName: String = String.Empty,
    val memberId: String = String.Empty,
    val coverage: String = String.Empty,
    val expirationDate: Long = Long.InvalidValue,
    val creationDate: String = String.Empty,
    val loggedInDid: String = String.Empty,
    val lastUsed: Long = Long.InvalidValue,
    val cardUrl: String? = String.Empty,
    override val iconUrl: String? = String.Empty
) : QrCode(issuer = issuer)