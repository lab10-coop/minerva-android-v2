package minerva.android.walletmanager.walletconfig

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.BlockchainRepository
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.IdentityPayload
import minerva.android.configProvider.model.ValuePayload
import minerva.android.configProvider.model.WalletConfigPayload
import minerva.android.configProvider.model.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_ARTIS_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_ETHEREUM_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigFields.Companion.DEFAULT_IDENTITY_NAME
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.DEFAULT_VERSION
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.FIRST_VALUES_INDEX
import minerva.android.walletmanager.model.DefaultWalletConfigIndexes.Companion.SECOND_VALUES_INDEX

class WalletConfigRepository(
    private val cryptographyRepository: CryptographyRepository,
    private val blockchainRepository: BlockchainRepository,
    private val localWalletProvider: LocalWalletConfigProvider,
    private val minervaApi: MinervaApi
) {
    private var currentWalletConfigVersion = Int.InvalidIndex

    fun loadWalletConfig(masterKey: MasterKey): Observable<WalletConfig> =
        Observable.mergeDelayError(
            localWalletProvider.loadWalletConfig()
                .toObservable()
                .doOnNext { currentWalletConfigVersion = it.version }
                .flatMap { completeKeys(masterKey, it) },
            minervaApi.getWalletConfig(publicKey = encodePublicKey(masterKey.publicKey))
                .toObservable()
                .filter { it.walletPayload.version > currentWalletConfigVersion }
                .doOnNext {
                    currentWalletConfigVersion = it.walletPayload.version
                    saveWalletConfigLocally(it.walletPayload)
                }
                .flatMap { completeKeys(masterKey, it.walletPayload) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        )

    fun getWalletConfig(masterKey: MasterKey): Single<WalletConfigResponse> =
        minervaApi.getWalletConfig(publicKey = encodePublicKey(masterKey.publicKey))


    fun saveWalletConfigLocally(walletConfigPayload: WalletConfigPayload) =
        localWalletProvider.saveWalletConfig(walletConfigPayload)

    fun updateWalletConfig(masterKey: MasterKey, walletConfigPayload: WalletConfigPayload) =
        minervaApi.saveWalletConfig(
            publicKey = encodePublicKey(masterKey.publicKey),
            walletConfigPayload = walletConfigPayload
        )

    fun createWalletConfig(masterKey: MasterKey) = updateWalletConfig(masterKey, createDefaultWalletConfig())

    fun encodePublicKey(publicKey: String) = publicKey.replace(SLASH, ENCODED_SLASH)

    fun createDefaultWalletConfig() =
        WalletConfigPayload(
            DEFAULT_VERSION, listOf(IdentityPayload(FIRST_IDENTITY_INDEX, DEFAULT_IDENTITY_NAME)),
            listOf(
                ValuePayload(FIRST_VALUES_INDEX, DEFAULT_ARTIS_NAME, Network.ARTIS.short),
                ValuePayload(SECOND_VALUES_INDEX, DEFAULT_ETHEREUM_NAME, Network.ETHEREUM.short)
            )
        )

    private fun completeKeys(masterKey: MasterKey, walletConfigPayload: WalletConfigPayload): Observable<WalletConfig> =
        walletConfigPayload.identityResponse.let { identitiesResponse ->
            walletConfigPayload.valueResponse.let { valuesResponse ->
                Observable.range(START, identitiesResponse.size)
                    .flatMapSingle { cryptographyRepository.computeDeliveredKeys(masterKey.privateKey, identitiesResponse[it].index) }
                    .toList()
                    .map { completeIdentitiesKeys(walletConfigPayload, it) }
                    .zipWith(Observable.range(START, valuesResponse.size)
                        .flatMapSingle { cryptographyRepository.computeDeliveredKeys(masterKey.privateKey, valuesResponse[it].index) }
                        .toList()
                        .map { completeValues(walletConfigPayload, it) },
                        BiFunction { identity: List<Identity>, value: List<Value> ->
                            WalletConfig(
                                walletConfigPayload.version,
                                identity,
                                value,
                                mapServicesResponseToServices(walletConfigPayload.serviceResponse)
                            )
                        }
                    ).toObservable()
            }
        }

    private fun completeIdentitiesKeys(walletConfigPayload: WalletConfigPayload, list: List<Triple<Int, String, String>>): List<Identity> {
        val identities = mutableListOf<Identity>()
        list.forEach {
            walletConfigPayload.getIdentityPayload(it.first).apply {
                identities.add(mapIdentityPayloadToIdentity(this, it.second, it.third))
            }
        }
        return identities
    }

    private fun completeValues(walletConfigPayload: WalletConfigPayload, list: List<Triple<Int, String, String>>): List<Value> {
        val values = mutableListOf<Value>()
        list.forEach {
            walletConfigPayload.getValuePayload(it.first).apply {
                values.add(mapValueResponseToValue(this, it.second, it.third, blockchainRepository.completeAddress(it.third)))
            }
        }
        return values
    }

    companion object {
        private const val START = 0
        const val SLASH = "/"
        const val ENCODED_SLASH = "%2F"
    }
}

enum class Network(val full: String, val short: String) {
    ARTIS(NetworkNameFull.ATS, NetworkNameShort.ATS),
    ETHEREUM(NetworkNameFull.ETH, NetworkNameShort.ETH),
    POA(NetworkNameFull.POA, NetworkNameShort.POA),
    XDAI(NetworkNameFull.XDAI, NetworkNameShort.XDAI);

    companion object {
        private val map = values().associateBy(Network::short)
        fun fromString(type: String) = map[type] ?: throw IllegalStateException("Not supported Network!")
    }
}