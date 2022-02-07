package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class NftCollectionDetails(
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
    @SerializedName("override")
    val override: Boolean = false
)