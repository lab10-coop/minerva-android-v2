package minerva.android.walletmanager.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class CredentialRequest(
    @SerializedName("callbackURL")
    val callbackUrl: String = String.Empty,
    @SerializedName("credentialRequirements")
    val credentialRequirements: CredentialRequirements? = null,
    @SerializedName("service")
    val service: RequestedService = RequestedService(),
    @SerializedName("typ")
    val type: String = String.Empty
)