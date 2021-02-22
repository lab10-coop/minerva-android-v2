package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

//TODO change Price format to just double
data class Markets(
    @SerializedName(MarketIds.ETHEREUM)
    val ethPrice: Price? = null,
    @SerializedName(MarketIds.DAI)
    val daiPrice: Price? = null,
    @SerializedName(MarketIds.POA_NETWORK)
    val poaPrice: Price? = null
)