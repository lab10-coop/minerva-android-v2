package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName
import minerva.android.kotlinUtils.InvalidId

data class WalletConfigVersion(
    @SerializedName("version")
    var version: Int? = Int.InvalidId
)