package minerva.android.walletmanager.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class IconImage(
    @SerializedName("/")
    val url: String = String.Empty
)