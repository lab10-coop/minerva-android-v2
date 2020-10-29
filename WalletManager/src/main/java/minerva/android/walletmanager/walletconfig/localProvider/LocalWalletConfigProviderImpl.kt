package minerva.android.walletmanager.walletconfig.localProvider

import android.content.SharedPreferences
import com.google.gson.Gson
import io.reactivex.Single
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.kotlinUtils.NO_DATA

class LocalWalletConfigProviderImpl(private val sharedPreferences: SharedPreferences) : LocalWalletConfigProvider {
    override fun getWalletConfig(): Single<WalletConfigPayload> =
        Single.just(sharedPreferences.getString(WALLET_CONFIG, String.NO_DATA)).map { makeWalletConfig(it) }

    override fun saveWalletConfig(walletConfig: WalletConfigPayload) =
        sharedPreferences.edit().putString(WALLET_CONFIG, Gson().toJson(walletConfig)).apply()

    private fun makeWalletConfig(rawWalletConfig: String): WalletConfigPayload {
        return if (rawWalletConfig == String.NO_DATA) WalletConfigPayload()
        else Gson().fromJson(rawWalletConfig, WalletConfigPayload::class.java)
    }

    companion object {
        private const val WALLET_CONFIG = "wallet_config"
    }

}