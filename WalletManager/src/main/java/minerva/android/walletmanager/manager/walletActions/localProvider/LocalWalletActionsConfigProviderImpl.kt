package minerva.android.walletmanager.manager.walletActions.localProvider

import android.content.Context
import com.google.gson.Gson
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.kotlinUtils.NO_DATA

class LocalWalletActionsConfigProviderImpl(private val context: Context) : LocalWalletActionsConfigProvider {

    override fun loadWalletActionsConfig(): WalletActionsConfigPayload =
        makeWalletConfig(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(WALLET_ACTIONS_CONFIG, String.NO_DATA))

    private fun makeWalletConfig(walletActionsConfig: String?): WalletActionsConfigPayload {
        return if (walletActionsConfig == String.NO_DATA) WalletActionsConfigPayload()
        else Gson().fromJson(walletActionsConfig, WalletActionsConfigPayload::class.java)
    }

    override fun saveWalletActionsConfig(walletActionsConfigPayload: WalletActionsConfigPayload) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(WALLET_ACTIONS_CONFIG, Gson().toJson(walletActionsConfigPayload))
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "WalletActionsConfig"
        private const val WALLET_ACTIONS_CONFIG = "wallet_actions_config"
    }
}