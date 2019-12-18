package minerva.android.walletmanager.walletconfig

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.model.WalletConfig

class WalletConfigRepository(
    private val localWalletProvider: LocalWalletConfigProvider,
    private val onlineWalletProvider: OnlineWalletConfigProvider
) {

    private var localRawWalletConfig: String = String.NO_DATA

    fun loadWalletConfig(): Observable<WalletConfig> = localWalletProvider.loadWalletConfigRaw()
        .map {
            localRawWalletConfig = it
            makeWalletConfig(it)
        }
        .mergeWith(
            onlineWalletProvider.loadWalletConfigRaw()
                .filter { it != localRawWalletConfig }
                .map {
                    makeWalletConfig(it)
                    //TODO save data to Local storage
                })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())


    private fun makeWalletConfig(rawWalletConfig: String): WalletConfig {
        return if (rawWalletConfig == String.NO_DATA) WalletConfig()
        else Gson().fromJson(rawWalletConfig, WalletConfig::class.java)

    }

    fun saveWalletConfig(walletConfig: WalletConfig) {
        //TODO implement saving data to thr server and local
//        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
//            putString(WALLET_CONFIG, walletConfig)
//            apply()
//        }
    }

    companion object {
        private const val PREFS_NAME = "WalletConfig"
        private const val WALLET_CONFIG = "wallet_config"
    }
}