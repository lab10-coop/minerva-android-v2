package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.crypto.hexToBigDecimal
import java.math.BigDecimal

data class GasPricesFromRpcOverHttp(
    @SerializedName("jsonrpc")
    private val _jsonrpc: String? = String.Empty,
    @SerializedName("id")
    private val _id: Int,
    @SerializedName("result")
    private val _result:  String? = String.Empty

) {
    val jsonrpc: String get() = _jsonrpc ?: String.Empty
    val id: Int get() = _id
    val result: BigDecimal? get() = hexToBigDecimal(_result ?: String.Empty)
}