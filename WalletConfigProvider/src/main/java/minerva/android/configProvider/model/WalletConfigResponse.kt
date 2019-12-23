package minerva.android.configProvider.model

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class WalletConfigResponse(
    @SerializedName("state")
    private val _state: String?,
    @SerializedName("message")
    private val _message: String?,
    @SerializedName("data")
    private val _walletConfigPayload: WalletConfigPayload? = WalletConfigPayload()
) {
    val state: String
        get() = _state ?: String.Empty
    val message: String
        get() = _message ?: String.Empty
    val walletPayload: WalletConfigPayload
        get() = _walletConfigPayload ?: WalletConfigPayload()
}