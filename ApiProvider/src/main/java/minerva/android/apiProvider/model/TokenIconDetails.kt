package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class TokenIconDetails(
    @SerializedName("chainId")
    val chainId: Int,
    @SerializedName("address")
    val address: String,
    @SerializedName("logoURI")
    val logoURI: String
)