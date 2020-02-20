package minerva.android.walletmanager.manager.wallet.walletconfig.localProvider

import android.content.Context
import com.google.gson.Gson
import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.kotlinUtils.NO_DATA

class LocalWalletConfigProviderImpl(private val context: Context) :
    LocalWalletConfigProvider {
    override fun loadWalletConfig(): Single<WalletConfigPayload> =
        Single.just(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(WALLET_CONFIG, String.NO_DATA)
        ).map { makeWalletConfig(it) }

    override fun saveWalletConfig(walletConfig: WalletConfigPayload) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(WALLET_CONFIG, Gson().toJson(walletConfig))
            apply()
        }
    }

    private fun makeWalletConfig(rawWalletConfig: String): WalletConfigPayload {
        return if (rawWalletConfig == String.NO_DATA) WalletConfigPayload()
        else Gson().fromJson(rawWalletConfig, WalletConfigPayload::class.java)
    }

    companion object {
        private const val PREFS_NAME = "WalletConfig"
        private const val WALLET_CONFIG = "wallet_config"
    }

}