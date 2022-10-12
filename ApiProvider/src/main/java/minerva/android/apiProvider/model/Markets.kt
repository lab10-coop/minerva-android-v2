package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName

data class Markets(
    @SerializedName(MarketIds.ETHEREUM)
    val ethFiatPrice: FiatPrice? = null,
    @SerializedName(MarketIds.GNO)
    val daiFiatPrice: FiatPrice? = null,
    @SerializedName(MarketIds.POA_NETWORK)
    val poaFiatPrice: FiatPrice? = null,
    @SerializedName(MarketIds.MATIC)
    val maticFiatPrice: FiatPrice? = null,
    @SerializedName(MarketIds.BSC_COIN)
    val bscFiatPrice: FiatPrice? = null,
    @SerializedName(MarketIds.RSK)
    val rskFiatPrice: FiatPrice? = null,
    @SerializedName(MarketIds.CELO)
    val celoFiatPrice: FiatPrice? = null,
    @SerializedName(MarketIds.AVAX)
    val avaxFiatPrice: FiatPrice? = null
)