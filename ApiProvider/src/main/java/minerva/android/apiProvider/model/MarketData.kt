package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class MarketData(
    @SerializedName("current_price")
    val currentPrice: Price = Price()
)