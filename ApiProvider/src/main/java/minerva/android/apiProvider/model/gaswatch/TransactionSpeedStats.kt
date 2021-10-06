package minerva.android.apiProvider.model.gaswatch

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal


data class TransactionSpeedStats(
    @SerializedName("gwei") var gwei: BigDecimal = BigDecimal.ZERO,
    @SerializedName("usd") var usd: Double = Double.NaN
)