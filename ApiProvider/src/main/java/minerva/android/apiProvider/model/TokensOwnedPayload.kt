package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class TokensOwnedPayload(
    @SerializedName("message")
    val message: String,
    @SerializedName("result")
    val result: List<TokenOwned>,
    @SerializedName("status")
    val status: String
) {
    data class TokenOwned(
        @SerializedName("balance")
        val balance: String,
        @SerializedName("contractAddress")
        val contractAddress: String,
        @SerializedName("decimals")
        val decimals: String,
        @SerializedName("eventsSeen")
        val eventsSeen: List<String>,
        @SerializedName("id")
        val id: String,
        @SerializedName("name")
        val name: String?,
        @SerializedName("symbol")
        val symbol: String?,
        @SerializedName("tokenURI")
        val tokenURI: String,
        @SerializedName("types")
        val types: List<String>
    )
}
