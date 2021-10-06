package minerva.android.apiProvider.model.gaswatch

import com.google.gson.annotations.SerializedName
import minerva.android.apiProvider.model.TransactionSpeed
import java.math.BigDecimal


data class GasPrices(
    @SerializedName("slow") var slow: TransactionSpeedStats = TransactionSpeedStats(),
    @SerializedName("normal") var normal: TransactionSpeedStats = TransactionSpeedStats(),
    @SerializedName("fast") var fast: TransactionSpeedStats = TransactionSpeedStats(),
    @SerializedName("instant") var instant: TransactionSpeedStats = TransactionSpeedStats(),
    @SerializedName("ethPrice") var ethPrice: Double = Double.NaN,
    @SerializedName("lastUpdated") var lastUpdated: BigDecimal = BigDecimal.ZERO,
    @SerializedName("sources") var sources: List<Sources> = emptyList()
)