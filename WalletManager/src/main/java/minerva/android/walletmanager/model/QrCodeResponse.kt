package minerva.android.walletmanager.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import minerva.android.kotlinUtils.Empty
import java.io.Serializable

@Parcelize
data class QrCodeResponse(
    var serviceName: String = String.Empty,
    var callback: String? = String.Empty,
    var issuer: String = String.Empty,
    var requestedData: ArrayList<String> = arrayListOf(),
    var identityFields: String = String.Empty
) : Parcelable