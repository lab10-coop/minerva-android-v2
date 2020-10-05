package minerva.android.walletmanager.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class RequestedService(
    @SerializedName("name")
    var name: String = String.Empty,
    @SerializedName("iconImage")
    val iconUrl: IconImage = IconImage()
)