package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class CurrentMarketPrice(
    @SerializedName("eur")
    val eur: String = String.Empty
)