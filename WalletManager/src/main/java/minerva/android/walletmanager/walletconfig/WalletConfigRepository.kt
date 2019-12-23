package minerva.android.walletmanager.walletconfig

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.IdentityPayload
import minerva.android.configProvider.model.ValuePayload
import minerva.android.configProvider.model.WalletConfigPayload
import minerva.android.configProvider.model.WalletConfigResponse
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.mapWalletConfigResponseToWalletConfig

class WalletConfigRepository(
    private val localWalletProvider: LocalWalletConfigProvider,
    private val minervaApiProvider: MinervaApi
) {

    private var localWalletConfig: WalletConfig = WalletConfig()

    fun loadWalletConfig(publicKey: String): Observable<WalletConfig> {
        return Observable.mergeDelayError(
            localWalletProvider.loadWalletConfig()
                .doOnNext {
                    localWalletConfig = it
                },
            minervaApiProvider.getWalletConfig(publicKey = publicKey)
                .filter { it.walletPayload.version > localWalletConfig.version }
                .map {
                    val walletConfig = mapWalletConfig(it)
                    saveWalletConfigLocally(walletConfig)
                    walletConfig
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        )
    }

    fun saveWalletConfigLocally(walletConfig: WalletConfig) =
        localWalletProvider.saveWalletConfig(walletConfig)

    private fun mapWalletConfig(walletConfigResponse: WalletConfigResponse) = mapWalletConfigResponseToWalletConfig(walletConfigResponse)

    fun createDefaultWalletConfig(masterKey: MasterKey) =
        minervaApiProvider.saveWalletConfig(publicKey = encodePublicKey(masterKey), walletConfigPayload = createDefaultWalletPayload())


    fun encodePublicKey(masterKey: MasterKey) = masterKey.publicKey.replace(SLASH, ENCODED_SLASH)

    private fun createDefaultWalletPayload() =
        WalletConfigPayload(
            DEFAULT_VERSION,
            listOf(IdentityPayload(FIRST_IDENTITY_INDEX)),
            listOf(ValuePayload(FIRST_VALUES_INDEX), ValuePayload(SECOND_VALUES_INDEX))
        )


    companion object {
        const val DEFAULT_VERSION = 0
        const val FIRST_IDENTITY_INDEX = 0
        const val FIRST_VALUES_INDEX = 1
        const val SECOND_VALUES_INDEX = 2
        const val SLASH = "/"
        const val ENCODED_SLASH = "%2F"
    }
}