package minerva.android.walletmanager.model.minervaprimitives.credential

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class CredentialRequirements(
    @SerializedName("type")
    val type: List<String> = listOf(),
    @SerializedName("constraints")
    val constraints: List<String> = listOf(),
    @SerializedName("reason")
    val reason: String = String.Empty
)