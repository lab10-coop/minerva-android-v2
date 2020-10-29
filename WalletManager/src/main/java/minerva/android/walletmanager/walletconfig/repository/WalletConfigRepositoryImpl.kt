package minerva.android.walletmanager.walletconfig.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.mapper.BitmapMapper
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.mappers.CredentialsPayloadToCredentials
import minerva.android.walletmanager.model.mappers.ServicesResponseToServicesMapper
import minerva.android.walletmanager.model.mappers.mapAccountResponseToAccount
import minerva.android.walletmanager.model.mappers.mapIdentityPayloadToIdentity
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProvider

class WalletConfigRepositoryImpl(
    private val cryptographyRepository: CryptographyRepository,
    private val localWalletProvider: LocalWalletConfigProvider,
    private val localStorage: LocalStorage,
    private val minervaApi: MinervaApi
) : WalletConfigRepository {

    private var currentWalletConfigVersion = Int.InvalidIndex

    override fun getWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig> =
        Observable.mergeDelayError(
            localWalletProvider.getWalletConfig()
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
        ).onErrorResumeNext { _: Observer<in WalletConfig> ->
            localWalletProvider.getWalletConfig()
                .toObservable()
                .flatMap { completeKeys(masterSeed, it) }
        }

    override fun restoreWalletConfig(masterSeed: MasterSeed): Single<WalletConfigResponse> =
        minervaApi.getWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey))

    override fun updateWalletConfig(masterSeed: MasterSeed, walletConfigPayload: WalletConfigPayload): Completable =
        minervaApi.saveWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey), walletConfigPayload = walletConfigPayload)

    override fun saveWalletConfigLocally(walletConfigPayload: WalletConfigPayload) =
        localWalletProvider.saveWalletConfig(walletConfigPayload)

    private fun completeKeys(masterSeed: MasterSeed, payload: WalletConfigPayload): Observable<WalletConfig> =
        payload.identityResponse.let { identitiesResponse ->
            payload.accountResponse.let { accountsResponse ->
                Observable.range(START, identitiesResponse.size)
                    .filter { !identitiesResponse[it].isDeleted }
                    .flatMapSingle { cryptographyRepository.computeDeliveredKeys(masterSeed.seed, identitiesResponse[it].index) }
                    .toList()
                    .map { completeIdentitiesKeys(payload, it) }
                    .map { completeIdentitiesProfileImages(it) }
                    .zipWith(Observable.range(START, accountsResponse.size)
                        .filter { !accountsResponse[it].isDeleted }
                        .flatMapSingle { cryptographyRepository.computeDeliveredKeys(masterSeed.seed, accountsResponse[it].index) }
                        .toList()
                        .map { completeAccountsKeys(payload, it) },
                        BiFunction { identity: List<Identity>, account: List<Account> ->
                            WalletConfig(
                                payload.version,
                                identity,
                                account,
                                ServicesResponseToServicesMapper.map(payload.serviceResponse),
                                CredentialsPayloadToCredentials.map(payload.credentialResponse)
                            )
                        }
                    ).toObservable()
            }
        }

    private fun completeIdentitiesKeys(walletConfigPayload: WalletConfigPayload, keys: List<DerivedKeys>): List<Identity> {
        val identities = mutableListOf<Identity>()
        walletConfigPayload.identityResponse.forEach { identityResponse ->
            getKeys(identityResponse.index, keys).let { key ->
                identities.add(mapIdentityPayloadToIdentity(identityResponse, key.publicKey, key.privateKey, key.address))
            }
        }
        return identities
    }

    private fun completeIdentitiesProfileImages(identities: List<Identity>): List<Identity> {
        identities.forEach {
            it.profileImageBitmap = BitmapMapper.fromBase64(localStorage.getProfileImage(it.profileImageName))
        }
        return identities
    }

    private fun completeAccountsKeys(walletConfigPayload: WalletConfigPayload, keys: List<DerivedKeys>): List<Account> {
        val accounts = mutableListOf<Account>()
        walletConfigPayload.accountResponse.forEach { accountResponse ->
            getKeys(accountResponse.index, keys).let { key ->
                val address = if (accountResponse.contractAddress.isEmpty()) key.address else accountResponse.contractAddress
                accounts.add(mapAccountResponseToAccount(accountResponse, key.publicKey, key.privateKey, address))
            }
        }
        return accounts
    }

    private fun getKeys(index: Int, keys: List<DerivedKeys>): DerivedKeys {
        keys.forEach {
            if (it.index == index) return it
        }
        return DerivedKeys(index, String.Empty, String.Empty, String.Empty)
    }

    companion object {
        private const val START = 0
    }
}