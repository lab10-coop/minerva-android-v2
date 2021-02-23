package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import java.math.BigDecimal

data class GasPrices(
    @SerializedName("code")
    private val _code: String? = String.Empty,
    @SerializedName("data")
    private val _speed: TransactionSpeed = TransactionSpeed()

) {
    val code: String get() = _code ?: String.Empty
    val speed: TransactionSpeed get() = _speed
}