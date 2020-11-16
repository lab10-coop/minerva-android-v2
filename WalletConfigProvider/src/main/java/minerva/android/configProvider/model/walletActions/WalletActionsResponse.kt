package minerva.android.configProvider.model.walletActions

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.Empty

data class WalletActionsResponse(
    @SerializedName("state")
    val state: String? = String.Empty,
    @SerializedName("message")
    private val _message: String? = String.Empty,
    @SerializedName("data")
    private val _walletActionsConfigPayload: WalletActionsConfigPayload? = WalletActionsConfigPayload()
) {
    val walletActionsConfigPayload: WalletActionsConfigPayload
        get() = _walletActionsConfigPayload ?: WalletActionsConfigPayload()
}