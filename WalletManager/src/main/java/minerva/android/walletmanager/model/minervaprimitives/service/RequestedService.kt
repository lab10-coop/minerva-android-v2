package minerva.android.walletmanager.model.minervaprimitives.service

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.IconImage

data class RequestedService(
    @SerializedName("issuer")
    var issuer: String = String.Empty,
    @SerializedName("name")
    var name: String = String.Empty,
    @SerializedName("iconImage")
    val iconUrl: IconImage = IconImage()
)