package minerva.android.apiProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class RpcOverHttpPayload(
    @SerializedName("jsonrpc")
    private val _jsonrpc: String? = String.Empty,
    @SerializedName("id")
    private val _id: Int,
    @SerializedName("method")
    private val _method: String? = String.Empty
) {
    val jsonrpc: String get() = _jsonrpc ?: String.Empty
    val id: Int get() = _id
    val method: String get() = _method ?: String.Empty

    companion object {
        private const val DEFAULT_JSON_RPC_VERSION = "2.0"
        private const val DEFAULT_ID = 1
        private const val METHOD_GET_GAS_PRICE = "eth_gasPrice"

        val GET_GAS_PRICE_PAYLOAD = RpcOverHttpPayload(DEFAULT_JSON_RPC_VERSION, DEFAULT_ID, METHOD_GET_GAS_PRICE)
    }
}