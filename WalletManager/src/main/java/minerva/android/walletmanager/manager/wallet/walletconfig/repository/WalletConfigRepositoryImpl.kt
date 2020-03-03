package minerva.android.walletmanager.manager.wallet.walletconfig.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.BlockchainRepository
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.ValuePayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.manager.wallet.walletconfig.localProvider.LocalWalletConfigProvider
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.DEFAULT_IDENTITY_NAME
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.DEFAULT_VERSION
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_VALUES_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_VALUES_INDEX
import minerva.android.walletmanager.model.defs.NetworkFullName
import minerva.android.walletmanager.model.mappers.mapIdentityPayloadToIdentity
import minerva.android.walletmanager.model.mappers.mapServicesResponseToServices
import minerva.android.walletmanager.model.mappers.mapValueResponseToValue
import minerva.android.walletmanager.utils.CryptoUtils
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey

class WalletConfigRepositoryImpl(
    private val cryptographyRepository: CryptographyRepository,
    private val blockchainRepository: BlockchainRepository,
    private val localWalletProvider: LocalWalletConfigProvider,
    private val minervaApi: MinervaApi
) : WalletConfigRepository {
    private var currentWalletConfigVersion = Int.InvalidIndex

    override fun loadWalletConfig(masterKey: MasterKey): Observable<WalletConfig> =
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

    override fun getWalletConfig(masterKey: MasterKey): Single<WalletConfigResponse> =
        minervaApi.getWalletConfig(publicKey = encodePublicKey(masterKey.publicKey))


    override fun saveWalletConfigLocally(walletConfigPayload: WalletConfigPayload) =
        localWalletProvider.saveWalletConfig(walletConfigPayload)

    override fun updateWalletConfig(masterKey: MasterKey, walletConfigPayload: WalletConfigPayload): Completable =
        minervaApi.saveWalletConfig(
            publicKey = encodePublicKey(masterKey.publicKey),
            walletConfigPayload = walletConfigPayload
        )

    override fun createWalletConfig(masterKey: MasterKey) = updateWalletConfig(masterKey, createDefaultWalletConfig())

    override fun createDefaultWalletConfig() =
        WalletConfigPayload(
            DEFAULT_VERSION, listOf(
                IdentityPayload(
                    FIRST_IDENTITY_INDEX,
                    DEFAULT_IDENTITY_NAME
                )
            ),
            listOf(
                ValuePayload(
                    FIRST_VALUES_INDEX,
                    CryptoUtils.prepareName(Network.ARTIS, 1),
                    Network.ARTIS.short
                ),
                ValuePayload(
                    SECOND_VALUES_INDEX,
                    CryptoUtils.prepareName(Network.ETHEREUM, 2),
                    Network.ETHEREUM.short
                )
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
                identities.add(
                    mapIdentityPayloadToIdentity(
                        this,
                        it.second,
                        it.third
                    )
                )
            }
        }
        return identities
    }

    private fun completeValues(walletConfigPayload: WalletConfigPayload, list: List<Triple<Int, String, String>>): List<Value> {
        val values = mutableListOf<Value>()
        list.forEach {
            walletConfigPayload.getValuePayload(it.first).apply {
                values.add(
                    mapValueResponseToValue(
                        this,
                        it.second,
                        it.third,
                        blockchainRepository.completeAddress(it.third)
                    )
                )
            }
        }
        return values
    }

    companion object {
        private const val START = 0
    }
}