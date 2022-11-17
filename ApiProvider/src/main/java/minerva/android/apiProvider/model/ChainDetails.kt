package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class ChainDetails(
    @SerializedName("name") var name: String,
    @SerializedName("chainId") var chainId: String,
    @SerializedName("shortName") var shortName: String,
    @SerializedName("networkId") var networkId: String,
    @SerializedName("nativeCurrency") var nativeCurrency: NativeCurrency,
    @SerializedName("rpc") var rpc: List<String>,
    @SerializedName("faucets") var faucets: List<String>,
    @SerializedName("infoURL") var infoURL: String
)
