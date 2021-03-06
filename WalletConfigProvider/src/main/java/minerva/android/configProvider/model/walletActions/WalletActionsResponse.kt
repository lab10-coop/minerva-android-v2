package minerva.android.configProvider.model.walletActions

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class WalletActionsResponse(
    @SerializedName("state")
    private val _state: String? = String.Empty,
    @SerializedName("message")
    private val _message: String? = String.Empty,
    @SerializedName("data")
    private val _walletActionsConfigPayload: WalletActionsConfigPayload? = WalletActionsConfigPayload()
) {
    val state: String
        get() = _state ?: String.Empty
    val message: String
        get() = _message ?: String.Empty
    val walletActionsConfigPayload: WalletActionsConfigPayload
        get() = _walletActionsConfigPayload ?: WalletActionsConfigPayload()
}