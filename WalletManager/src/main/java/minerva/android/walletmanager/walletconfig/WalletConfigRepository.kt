package minerva.android.walletmanager.walletconfig

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.WalletConfigResponse
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_ARTIS_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_ETHEREUM_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_IDENTITY_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.DEFAULT_VERSION
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.FIRST_VALUES_INDEX
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.SECOND_VALUES_INDEX
import java.lang.IllegalStateException

class WalletConfigRepository(
    private val localWalletProvider: LocalWalletConfigProvider,
    private val minervaApi: MinervaApi
) {
    private var currentWalletConfigVersion = Int.InvalidIndex

    fun loadWalletConfig(publicKey: String): Observable<WalletConfig> {
        return Observable.mergeDelayError(
            localWalletProvider.loadWalletConfig()
                .doOnNext { currentWalletConfigVersion = it.version },
            minervaApi.getWalletConfig(publicKey = encodePublicKey(publicKey))
                .toObservable()
                .filter { it.walletPayload.version > currentWalletConfigVersion }
                .map { saveLocallyAndMapToWalletConfig(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        )
    }

    fun getWalletConfig(masterKey: MasterKey): Single<WalletConfigResponse> =
        minervaApi.getWalletConfig(publicKey = encodePublicKey(masterKey.publicKey))


    fun saveWalletConfigLocally(walletConfig: WalletConfig) =
        localWalletProvider.saveWalletConfig(walletConfig)

    fun updateWalletConfig(masterKey: MasterKey, walletConfig: WalletConfig) =
        minervaApi.saveWalletConfig(
            publicKey = encodePublicKey(masterKey.publicKey),
            walletConfigPayload = mapWalletConfigToWalletPayload(walletConfig)
        )

    fun createWalletConfig(masterKey: MasterKey) = updateWalletConfig(masterKey, createDefaultWalletConfig())

    fun encodePublicKey(publicKey: String) = publicKey.replace(SLASH, ENCODED_SLASH)

    fun createDefaultWalletConfig() =
        WalletConfig(
            DEFAULT_VERSION, listOf(Identity(index = FIRST_IDENTITY_INDEX, name = DEFAULT_IDENTITY_NAME)),
            listOf(
                Value(index = FIRST_VALUES_INDEX, name = DEFAULT_ARTIS_NAME, network = Network.ARTIS.value),
                Value(index = SECOND_VALUES_INDEX, name = DEFAULT_ETHEREUM_NAME, network = Network.ETHEREUM.value)
            )
        )

    private fun saveLocallyAndMapToWalletConfig(walletConfigResponse: WalletConfigResponse): WalletConfig {
        val walletConfig = mapWalletConfigResponseToWalletConfig(walletConfigResponse)
        saveWalletConfigLocally(walletConfig)
        currentWalletConfigVersion = walletConfig.version
        return walletConfig
    }

    companion object {
        const val SLASH = "/"
        const val ENCODED_SLASH = "%2F"
    }
}

enum class Network(val value: String) {
    ARTIS(Networks.ATS),
    ETHEREUM(Networks.ETH),
    POA(Networks.POA),
    XDAI(Networks.XDAI);

    companion object {
        private val map = values().associateBy(Network::value)
        fun fromString(type: String) = map[type] ?: throw IllegalStateException("Not supported Network!")
    }
}