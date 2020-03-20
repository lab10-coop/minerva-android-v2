package minerva.android.walletmanager.model

import minerva.android.kotlinUtils.Empty
import java.io.Serializable

data class QrCodeResponse(
    var serviceName: String = String.Empty,
    var callback: String? = String.Empty,
    var issuer: String = String.Empty,
    var requestedData: ArrayList<String> = arrayListOf(),
    var identityFields: String = String.Empty
) : Serializable