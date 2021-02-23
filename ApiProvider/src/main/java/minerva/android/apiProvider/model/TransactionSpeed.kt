package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TransactionSpeed(
    @SerializedName("rapid")
    val rapid: BigDecimal = BigDecimal.ZERO,
    @SerializedName("fast")
    val fast: BigDecimal = BigDecimal.ZERO,
    @SerializedName("slow")
    val slow: BigDecimal = BigDecimal.ZERO,
    @SerializedName("standard")
    val standard: BigDecimal = BigDecimal.ZERO,
    @SerializedName("timestamp")
    val timestamp: BigDecimal = BigDecimal.ZERO
)
