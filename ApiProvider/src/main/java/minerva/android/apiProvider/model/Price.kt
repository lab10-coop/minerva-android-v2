package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidValue

data class Price(
    @SerializedName("eur")
    val value: Double? = Double.InvalidValue
)