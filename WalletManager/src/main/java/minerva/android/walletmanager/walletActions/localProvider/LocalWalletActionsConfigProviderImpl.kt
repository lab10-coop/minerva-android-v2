package minerva.android.walletmanager.walletActions.localProvider

import android.content.SharedPreferences
import com.google.gson.Gson
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.kotlinUtils.NO_DATA

//TODO move saving WalletActionsConfigPayload to Room - MinervaDatabase
class LocalWalletActionsConfigProviderImpl(private val sharedPreferences: SharedPreferences) : LocalWalletActionsConfigProvider {

    override fun loadWalletActionsConfig(): WalletActionsConfigPayload =
        makeWalletConfig(sharedPreferences.getString(WALLET_ACTIONS_CONFIG, String.NO_DATA))

    private fun makeWalletConfig(walletActionsConfig: String?): WalletActionsConfigPayload {
        return if (walletActionsConfig == String.NO_DATA) WalletActionsConfigPayload()
        else Gson().fromJson(walletActionsConfig, WalletActionsConfigPayload::class.java)
    }

    override fun saveWalletActionsConfig(walletActionsConfigPayload: WalletActionsConfigPayload) {
        sharedPreferences.edit().putString(WALLET_ACTIONS_CONFIG, Gson().toJson(walletActionsConfigPayload)).apply()
    }

    companion object {
        private const val WALLET_ACTIONS_CONFIG = "wallet_actions_config"
    }
}