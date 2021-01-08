package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class GasPrice(
    @SerializedName("fast")
    private val _fast: BigDecimal? = BigDecimal.ZERO
) {
    val fast: BigDecimal get() = _fast ?: BigDecimal.ZERO
}