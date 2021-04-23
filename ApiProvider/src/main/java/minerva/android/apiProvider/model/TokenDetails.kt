package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue

data class TokenDetails(
    @SerializedName("chainId")
    val chainId: Int,
    @SerializedName("address")
    val address: String,
    @SerializedName("logoURI")
    val logoURI: String,
    @SerializedName("name")
    val name: String = String.Empty,
    @SerializedName("symbol")
    val symbol: String = String.Empty,
    @SerializedName("decimals")
    val decimals: Int = Int.InvalidValue,
    @SerializedName("tags")
    private val _tags: List<String>? = emptyList()
) {
    val tags: List<String> get() = _tags ?: emptyList()
}