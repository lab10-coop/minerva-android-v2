package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class DappDetails(
    @SerializedName("shortName")
    val shortName: String,
    @SerializedName("subtitle")
    val subtitle: String,
    @SerializedName("connectLink")
    val connectLink: String,
    @SerializedName("buttonColor")
    val buttonColor: String,
    @SerializedName("chainIds")
    val chainIds: List<Int> = emptyList(),
    @SerializedName("iconLink")
    val iconLink: String,
    @SerializedName("longName")
    val longName: String,
    @SerializedName("explainerTitle")
    val explainerTitle: String,
    @SerializedName("explainerText")
    val explainerText: String,
    @SerializedName("explainerStepByStep")
    val explainerStepByStep: List<String> = emptyList(),
    @SerializedName("sponsored") // 0 if not sponsored, 1 and more - sponsored order
    val sponsored: Int,
    @SerializedName("sponsoredChainId")
    val sponsoredChainId: Int
)
