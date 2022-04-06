package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class GasPrices(
    @SerializedName("fastest")
    val rapid: BigDecimal = BigDecimal.ZERO,
    @SerializedName("fast")
    val fast: BigDecimal = BigDecimal.ZERO,
    @SerializedName("safeLow")
    val slow: BigDecimal = BigDecimal.ZERO,
    @SerializedName("standard")
    val standard: BigDecimal = BigDecimal.ZERO
)

