package minerva.android.services.login.uitls

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.QrCodeResponse

@Parcelize
data class LoginPayload(
    val loginStatus: Int,
    val identityPublicKey: String = String.Empty,
    val qrCode: QrCodeResponse? = null
) : Parcelable