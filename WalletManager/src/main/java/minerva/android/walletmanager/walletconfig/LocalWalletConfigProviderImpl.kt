package minerva.android.walletmanager.walletconfig

import android.content.Context
import com.google.gson.Gson
import io.reactivex.Observable
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.model.WalletConfig

class LocalWalletConfigProviderImpl(private val context: Context) : LocalWalletConfigProvider {
    override fun loadWalletConfig(): Observable<WalletConfig> =
        Observable.just(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(WALLET_CONFIG, String.NO_DATA)
        ).map { makeWalletConfig(it) }

    override fun saveWalletConfig(walletConfig: WalletConfig) {
        val walletConfigRaw = Gson().toJson(walletConfig)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(WALLET_CONFIG, walletConfigRaw)
            apply()
        }
    }

    private fun makeWalletConfig(rawWalletConfig: String): WalletConfig {
        return if (rawWalletConfig == String.NO_DATA) WalletConfig()
        else Gson().fromJson(rawWalletConfig, WalletConfig::class.java)
    }

    companion object {
        private const val PREFS_NAME = "WalletConfig"
        private const val WALLET_CONFIG = "wallet_config"
    }

}