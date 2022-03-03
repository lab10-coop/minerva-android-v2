package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

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
        val _decimals: String?,
        @SerializedName("eventsSeen")
        val eventsSeen: List<String>,
        @SerializedName("id")
        val id: String,
        @SerializedName("name")
        val _name: String?,
        @SerializedName("symbol")
        val _symbol: String?,
        @SerializedName("tokenURI")
        val tokenURI: String?,
        @SerializedName("types")
        val types: List<String>,
        @SerializedName("tokenJson")
        val tokenJson: TokenJson? = TokenJson.Empty
    ) {

        companion object {
            private const val DEFAULT_DECIMALS = "0"
        }

        val name: String
            get() = _name ?: tokenJson?.name ?: String.Empty

        val symbol: String
            get() = _symbol ?: tokenJson?.symbol ?: String.Empty

        val decimals: String
            get() = _decimals ?: tokenJson?.decimals ?: DEFAULT_DECIMALS

        data class TokenJson(
            @SerializedName("decimals")
            val decimals: String?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("symbol")
            val symbol: String?,
            @SerializedName("image", alternate =  ["image_url"])
            var image: String?,
            @SerializedName("description")
            val description: String?,
            @SerializedName("animation_url")
            val animationUri: String?
        ) {
            companion object {
                val Empty = TokenJson(
                    null, null, null, null, null, null
                )
            }
        }
    }
}
