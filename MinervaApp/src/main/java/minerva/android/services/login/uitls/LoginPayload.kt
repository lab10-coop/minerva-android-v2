package minerva.android.services.login.uitls

import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.QrCodeResponse
import java.io.Serializable

data class LoginPayload(
    val loginStatus: Int,
    val identityPublicKey: String = String.Empty,
    val qrCode: QrCodeResponse? = null
) : Serializable