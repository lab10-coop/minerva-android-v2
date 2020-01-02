package minerva.android.walletmanager.walletconfig

import io.reactivex.Observable
import io.reactivex.Single
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
    private val minervaApi: MinervaApi
) {

    private var localWalletConfig: WalletConfig = WalletConfig()

    fun loadWalletConfig(publicKey: String): Observable<WalletConfig> {
        return Observable.mergeDelayError(
            localWalletProvider.loadWalletConfig()
                .doOnNext {
                    localWalletConfig = it
                },
            minervaApi.getWalletConfigObservable(publicKey = encodePublicKey(publicKey))
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
        minervaApi.saveWalletConfig(publicKey = encodePublicKey(masterKey.publicKey), walletConfigPayload = createDefaultWalletPayload())

    fun getWalletConfig(masterKey: MasterKey): Single<WalletConfigResponse> =
        minervaApi.getWalletConfig(publicKey = encodePublicKey(masterKey.publicKey))

    fun encodePublicKey(publicKey: String) = publicKey.replace(SLASH, ENCODED_SLASH)

    private fun createDefaultWalletPayload() =
        WalletConfigPayload(
            DEFAULT_VERSION,
            listOf(IdentityPayload(_index = FIRST_IDENTITY_INDEX, _name = DEFAULT_IDENTITY_NAME)),
            listOf(ValuePayload(_index = FIRST_VALUES_INDEX), ValuePayload(_index = SECOND_VALUES_INDEX))
        )


    companion object {
        const val DEFAULT_VERSION = 0
        const val FIRST_IDENTITY_INDEX = 0
        const val FIRST_VALUES_INDEX = 1
        const val SECOND_VALUES_INDEX = 2
        const val SLASH = "/"
        const val ENCODED_SLASH = "%2F"
        const val DEFAULT_IDENTITY_NAME = "Identity #1"
    }
}