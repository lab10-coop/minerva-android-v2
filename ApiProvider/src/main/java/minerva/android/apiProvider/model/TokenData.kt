package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class TokenData(
    @SerializedName("type")
    val type: String = String.Empty,
    @SerializedName("symbol")
    val symbol: String = String.Empty,
    @SerializedName("name")
    val name: String? = String.Empty,
    @SerializedName("decimals")
    val decimals: String = String.Empty,
    @SerializedName("contractAddress")
    val address: String = String.Empty,
    @SerializedName("balance")
    var balance: String = String.Empty
)