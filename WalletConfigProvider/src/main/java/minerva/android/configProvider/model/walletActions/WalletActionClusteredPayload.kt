package minerva.android.configProvider.model.walletActions

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidValue

data class WalletActionClusteredPayload(
    @SerializedName("lastUsed")
    private var _lastUsed: Long? = Long.InvalidValue,
    @SerializedName("walletActionsClustered")
    private var _clusteredActions: MutableList<WalletActionPayload>? = mutableListOf()
) {
    val lastUsed: Long get() = _lastUsed ?: Long.InvalidValue
    val clusteredActions: MutableList<WalletActionPayload> get() = _clusteredActions ?: mutableListOf()
}