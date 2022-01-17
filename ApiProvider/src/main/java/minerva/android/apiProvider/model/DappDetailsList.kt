package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class DappDetailsList(
    @SerializedName("version")
    val version: String,
    @SerializedName("dapps")
    val dappsList: List<DappDetails>
)