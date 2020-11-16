package minerva.android.configProvider.model.walletConfig

import com.google.gson.annotations.SerializedName

data class WalletConfigModelVersion(
    @SerializedName("modelVersion")
    var modelVersion: Double = INIT_MODEL_VERSION
)

const val INIT_MODEL_VERSION: Double = 1.0