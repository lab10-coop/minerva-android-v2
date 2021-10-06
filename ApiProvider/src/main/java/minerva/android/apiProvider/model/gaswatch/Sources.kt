package minerva.android.apiProvider.model.gaswatch

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal

data class Sources(

    @SerializedName("name") var name: String = String.Empty,
    @SerializedName("source") var source: String = String.Empty,
    @SerializedName("fast") var fast: BigDecimal = BigDecimal.ZERO,
    @SerializedName("standard") var standard: BigDecimal = BigDecimal.ZERO,
    @SerializedName("slow") var slow: BigDecimal = BigDecimal.ZERO,
    @SerializedName("lastBlock") var lastBlock: BigDecimal = BigDecimal.ZERO

)