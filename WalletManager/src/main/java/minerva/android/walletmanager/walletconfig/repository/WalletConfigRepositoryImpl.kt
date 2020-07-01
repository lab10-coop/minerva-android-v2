package minerva.android.walletmanager.walletconfig.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.mappers.mapIdentityPayloadToIdentity
import minerva.android.walletmanager.model.mappers.mapServicesResponseToServices
import minerva.android.walletmanager.model.mappers.mapValueResponseToValue
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProvider

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

    override fun createWalletConfig(masterSeed: MasterSeed) = updateWalletConfig(masterSeed)

    private fun completeKeys(masterSeed: MasterSeed, payload: WalletConfigPayload): Observable<WalletConfig> =
        payload.identityResponse.let { identitiesResponse ->
            payload.valueResponse.let { valuesResponse ->
                Observable.range(START, identitiesResponse.size)
                    .filter { !identitiesResponse[it].isDeleted }
                    .flatMapSingle { cryptographyRepository.computeDeliveredKeys(masterSeed.seed, identitiesResponse[it].index) }
                    .toList()
                    .map {
                        completeIdentitiesKeys(payload, it)
                    }
                    .zipWith(Observable.range(START, valuesResponse.size)
                        .filter { !valuesResponse[it].isDeleted }
                        .flatMapSingle { cryptographyRepository.computeDeliveredKeys(masterSeed.seed, valuesResponse[it].index) }
                        .toList()
                        .map { completeValues(payload, it) },
                        BiFunction { identity: List<Identity>, value: List<Value> ->
                            WalletConfig(payload.version, identity, value, mapServicesResponseToServices(payload.serviceResponse))
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