package minerva.android.walletmanager.model.minervaprimitives.credential

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.minervaprimitives.service.RequestedService

data class CredentialRequest(
    @SerializedName("issuer")
    val issuer: String = String.Empty,
    @SerializedName("callbackURL")
    val callbackUrl: String = String.Empty,
    @SerializedName("credentialRequirements")
    val credentialRequirements: CredentialRequirements? = null,
    @SerializedName("service")
    val service: RequestedService = RequestedService(),
    @SerializedName("typ")
    val type: String = String.Empty
)