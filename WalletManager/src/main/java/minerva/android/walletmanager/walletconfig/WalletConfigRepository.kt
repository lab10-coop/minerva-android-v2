package minerva.android.walletmanager.walletconfig

import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.IdentityPayload
import minerva.android.configProvider.model.ValuePayload
import minerva.android.configProvider.model.WalletConfigPayload
import minerva.android.kotlinUtils.NO_DATA
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import timber.log.Timber

class WalletConfigRepository(
    private val localWalletProvider: LocalWalletConfigProvider,
    private val onlineWalletProvider: OnlineWalletConfigProvider, //TODO delete when loading walletConfig is ready
    private val api: MinervaApi
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

    fun createDefaultWalletConfig(masterKey: MasterKey) =
        api.saveWalletConfig(publicKey = encodePublicKey(masterKey), walletConfigPayload = createDefaultWalletPayload())

    fun saveWalletConfigLocally(walletConfig: WalletConfig) {
//        TODO implement saving walletConfig to local storage
    }

    fun encodePublicKey(masterKey: MasterKey) = masterKey.publicKey.replace(SLASH, ENCODED_SLASH)

    private fun createDefaultWalletPayload() =
        WalletConfigPayload(
            DEFAULT_VERSION,
            listOf(IdentityPayload(FIRST_IDENTITY_INDEX)),
            listOf(ValuePayload(FIRST_VALUES_INDEX), ValuePayload(SECOND_VALUES_INDEX))
        )

    companion object {
        const val DEFAULT_VERSION = "0.0"
        const val FIRST_IDENTITY_INDEX = "0"
        const val FIRST_VALUES_INDEX = "1"
        const val SECOND_VALUES_INDEX = "2"
        const val SLASH = "/"
        const val ENCODED_SLASH = "%2F"
    }
}