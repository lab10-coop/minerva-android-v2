package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class TokenTxResponse(
    @SerializedName("status")
    val status: String = String.Empty,
    @SerializedName("result")
    val tokens: List<TokenTx> = listOf(),
    @SerializedName("message")
    val message: String = String.Empty
)
