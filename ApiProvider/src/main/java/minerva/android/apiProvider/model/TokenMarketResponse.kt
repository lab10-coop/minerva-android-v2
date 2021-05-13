package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class TokenMarketResponse(
    @SerializedName("id")
    val id: String = String.Empty,
    @SerializedName("name")
    val name: String = String.Empty,
    @SerializedName("market_data")
    val marketData: MarketData = MarketData()
)