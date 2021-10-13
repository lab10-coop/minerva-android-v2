package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName


data class NativeCurrency(
    @SerializedName("name") var name: String,
    @SerializedName("symbol") var symbol: String,
    @SerializedName("decimals") var decimals: Int
)