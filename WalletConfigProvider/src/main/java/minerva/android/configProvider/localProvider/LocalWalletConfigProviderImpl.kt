package minerva.android.configProvider.localProvider

import android.content.SharedPreferences
import com.google.gson.Gson
import io.reactivex.Single
import minerva.android.configProvider.migration.Migration
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.kotlinUtils.NO_DATA

//TODO move saving WalletConfig to Room - MinervaDatabase
class LocalWalletConfigProviderImpl(private val sharedPreferences: SharedPreferences) : LocalWalletConfigProvider {

    override fun getWalletConfig(): Single<WalletConfigPayload> =
        Single.just(sharedPreferences.getString(WALLET_CONFIG, String.NO_DATA))
            .map { makeWalletConfig(it) }

    override fun saveWalletConfig(payload: WalletConfigPayload): Single<WalletConfigPayload> =
        sharedPreferences.edit().putString(WALLET_CONFIG, Gson().toJson(payload)).apply().let {
            Single.just(payload)
        }

    private fun makeWalletConfig(rawWalletConfig: String): WalletConfigPayload =
        if (rawWalletConfig == String.NO_DATA) WalletConfigPayload()
        else Migration.migrateIfNeeded(rawWalletConfig)

    companion object {
        private const val WALLET_CONFIG = "wallet_config"
    }
}