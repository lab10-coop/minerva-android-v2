package minerva.android.configProvider.model.walletActions

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidId

data class WalletActionsConfigPayload(
    @SerializedName("version")
    private var _version: Int? = Int.InvalidId,
    @SerializedName("actions")
    private var _actions: MutableList<WalletActionClusteredPayload>? = mutableListOf()
) {
    val version: Int
        get() = _version ?: Int.InvalidId
    val actions: MutableList<WalletActionClusteredPayload>
        get() = _actions ?: mutableListOf()

    val updateVersion: Int
        get() = version + 1

}