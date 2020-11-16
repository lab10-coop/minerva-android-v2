package minerva.android.configProvider.migration

import com.google.gson.Gson
import minerva.android.configProvider.BuildConfig
import minerva.android.configProvider.error.IncompatibleModelThrowable
import minerva.android.configProvider.model.walletConfig.WalletConfigModelVersion
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload

object Migration {

    /*
    * Backward compatibility of WalletConfigPayload should be supported whenever corresponding model changes. Compatibility issues can occur:
    * 1. When getting wallet with old data model from local storage - (UPDATE APP WITH NEW VERSION WHEN MODEL CHANGES)
    * 2. When restoring wallet with old data model from server - (RESTORE WALLET)
    *
    * Steps how to support backward compatibility:
    * 1. When model changes increase model version in BuildConfig file in WalletConfigProvider module
    * 2. When model from server/local storage is ex. 1.0 and the current one is 2.0, parse the old model to WalletConfigPayload_1_0 supporting
    * version 1.0 and then map it to the newest supported WalletConfigPayload.
    * 3. Otherwise IncompatibleModelThrowable is thrown.
    * 4. Always the final/the most current version of model has the name -> WalletConfigPayload
    * */

    fun migrateIfNeeded(rawResponse: String): WalletConfigPayload {
        val modelVersion = getModelVersion(rawResponse)
        return if (modelVersion == BuildConfig.MODEL_VERSION) {
            Gson().fromJson(rawResponse, WalletConfigPayload::class.java).copy(modelVersion = modelVersion)
        } else {
            throw IncompatibleModelThrowable()
        }
    }

    private fun getModelVersion(rawWalletConfig: String) =
        Gson().fromJson(rawWalletConfig, WalletConfigModelVersion::class.java).modelVersion
}