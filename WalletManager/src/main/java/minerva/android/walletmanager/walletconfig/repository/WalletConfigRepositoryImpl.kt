package minerva.android.walletmanager.walletconfig.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletConfig.IdentityPayload
import minerva.android.configProvider.model.walletConfig.ValuePayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProvider
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.DefaultWalletConfigFields.Companion.DEFAULT_IDENTITY_NAME
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.DEFAULT_VERSION
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.FIRST_VALUES_INDEX
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes.Companion.SECOND_VALUES_INDEX
import minerva.android.walletmanager.model.mappers.mapIdentityPayloadToIdentity
import minerva.android.walletmanager.model.mappers.mapServicesResponseToServices
import minerva.android.walletmanager.model.mappers.mapValueResponseToValue
import minerva.android.walletmanager.utils.CryptoUtils
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey

class WalletConfigRepositoryImpl(
    private val cryptographyRepository: CryptographyRepository,
    private val localWalletProvider: LocalWalletConfigProvider,
    private val minervaApi: MinervaApi
) : WalletConfigRepository {
    private var currentWalletConfigVersion = Int.InvalidIndex

    override fun loadWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig> =
        Observable.mergeDelayError(
            localWalletProvider.loadWalletConfig()
                .toObservable()
                .doOnNext { currentWalletConfigVersion = it.version }
                .flatMap { completeKeys(masterSeed, it) },
            minervaApi.getWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey))
                .toObservable()
                .filter { it.walletPayload.version > currentWalletConfigVersion }
                .doOnNext {
                    currentWalletConfigVersion = it.walletPayload.version
                    saveWalletConfigLocally(it.walletPayload)
                }
                .flatMap { completeKeys(masterSeed, it.walletPayload) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        )

    override fun getWalletConfig(masterSeed: MasterSeed): Single<WalletConfigResponse> =
        minervaApi.getWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey))


    override fun saveWalletConfigLocally(walletConfigPayload: WalletConfigPayload) =
        localWalletProvider.saveWalletConfig(walletConfigPayload)

    override fun updateWalletConfig(masterSeed: MasterSeed, walletConfigPayload: WalletConfigPayload): Completable =
        minervaApi.saveWalletConfig(
            publicKey = encodePublicKey(masterSeed.publicKey),
            walletConfigPayload = walletConfigPayload
        )

    override fun createWalletConfig(masterSeed: MasterSeed) = updateWalletConfig(masterSeed, createDefaultWalletConfig())

    override fun createDefaultWalletConfig() =
        WalletConfigPayload(
            DEFAULT_VERSION, listOf(IdentityPayload(FIRST_IDENTITY_INDEX, DEFAULT_IDENTITY_NAME)),
            listOf(
                ValuePayload(FIRST_VALUES_INDEX, CryptoUtils.prepareName(Network.ARTIS, FIRST_VALUES_INDEX), Network.ARTIS.short),
                ValuePayload(SECOND_VALUES_INDEX, CryptoUtils.prepareName(Network.ETHEREUM, SECOND_VALUES_INDEX), Network.ETHEREUM.short)
            )
        )

    private fun completeKeys(masterSeed: MasterSeed, walletConfigPayload: WalletConfigPayload): Observable<WalletConfig> =
        walletConfigPayload.identityResponse.let { identitiesResponse ->
            walletConfigPayload.valueResponse.let { valuesResponse ->
                Observable.range(START, identitiesResponse.size)
                    .filter { !identitiesResponse[it].isDeleted }
                    .flatMapSingle { cryptographyRepository.computeDeliveredKeys(masterSeed.seed, identitiesResponse[it].index) }
                    .toList()
                    .map {
                        completeIdentitiesKeys(walletConfigPayload, it)
                    }
                    .zipWith(Observable.range(START, valuesResponse.size)
                        .filter { !valuesResponse[it].isDeleted }
                        .flatMapSingle { cryptographyRepository.computeDeliveredKeys(masterSeed.seed, valuesResponse[it].index) }
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

    private fun completeIdentitiesKeys(walletConfigPayload: WalletConfigPayload, list: List<DerivedKeys>): List<Identity> {
        val identities = mutableListOf<Identity>()
        list.forEach {
            walletConfigPayload.getIdentityPayload(it.index).apply {
                identities.add(mapIdentityPayloadToIdentity(this, it.publicKey, it.privateKey))
            }
        }
        return identities
    }

    private fun completeValues(walletConfigPayload: WalletConfigPayload, list: List<DerivedKeys>): List<Value> {
        val values = mutableListOf<Value>()
        list.forEach { keys ->
            walletConfigPayload.getValuePayload(keys.index).apply {
                val address = if (contractAddress.isEmpty()) keys.address else contractAddress
                values.add(mapValueResponseToValue(this, keys.publicKey, keys.privateKey, address))
            }
        }
        return values
    }

    companion object {
        private const val START = 0
    }
}