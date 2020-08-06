package minerva.android.walletmanager.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

@Parcelize
open class QrCodeResponse(
    open var issuer: String = String.Empty
) : Parcelable

@Parcelize
data class ServiceQrResponse(
    override var issuer: String = String.Empty,
    var serviceName: String = String.Empty,
    var callback: String? = String.Empty,
    var requestedData: ArrayList<String> = arrayListOf(),
    var identityFields: String = String.Empty
) : QrCodeResponse(issuer = issuer)

@Parcelize
data class CredentialQrResponse(
    override var issuer: String = String.Empty,
    val name: String = String.Empty,
    val type: String = String.Empty,
    val memberName: String = String.Empty,
    val memberId: String = String.Empty,
    val coverage: String = String.Empty,
    val expirationDate: Long = Long.InvalidValue,
    val creationDate: String = String.Empty,
    val loggedInDid: String = String.Empty,
    val lastUsed: Long = Long.InvalidValue
) : QrCodeResponse(issuer = issuer)