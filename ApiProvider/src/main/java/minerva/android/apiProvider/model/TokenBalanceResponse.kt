package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class TokenBalanceResponse(
    @SerializedName("status")
    val status: String = String.Empty,
    @SerializedName("result")
    val tokens: List<TokenBalance> = listOf(),
    @SerializedName("message")
    val message: String = String.Empty
)