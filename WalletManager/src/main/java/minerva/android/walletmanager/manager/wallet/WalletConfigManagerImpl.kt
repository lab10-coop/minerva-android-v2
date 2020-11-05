package minerva.android.walletmanager.manager.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.configProvider.repository.MinervaApiRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivedKeys
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.mapper.BitmapMapper
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.ResponseState
import minerva.android.walletmanager.model.mappers.*
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey
import minerva.android.walletmanager.utils.DefaultWalletConfig
import minerva.android.walletmanager.utils.handleAutomaticBackupFailedError
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProvider
import timber.log.Timber

class WalletConfigManagerImpl(
    private val keystoreRepository: KeystoreRepository,
    private val cryptographyRepository: CryptographyRepository,
    private val localWalletProvider: LocalWalletConfigProvider,
    private val localStorage: LocalStorage,
    private val minervaApi: MinervaApiRepository
) : WalletConfigManager {

    override lateinit var masterSeed: MasterSeed
    private var disposable: Disposable? = null

    private val _walletConfigLiveData = MutableLiveData<WalletConfig>()
    override val walletConfigLiveData: LiveData<WalletConfig> get() = _walletConfigLiveData

    private val _walletConfigErrorLiveData = MutableLiveData<Event<Throwable>>()
    override val walletConfigErrorLiveData: LiveData<Event<Throwable>> get() = _walletConfigErrorLiveData

    override fun getWalletConfig(): WalletConfig? = walletConfigLiveData.value
    override fun isMasterSeedSaved(): Boolean = keystoreRepository.isMasterSeedSaved()

    private var localWalletConfigVersion = Int.InvalidIndex

    override fun initWalletConfig() {
        keystoreRepository.decryptMasterSeed()?.let {
            masterSeed = it
            loadWalletConfig(it)
        }.orElse { _walletConfigErrorLiveData.value = Event(Throwable()) }
    }

    private fun loadWalletConfig(masterSeed: MasterSeed) {
        disposable = fetchWalletConfig(masterSeed)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { _walletConfigLiveData.value = it },
                onError = { Timber.e("Loading WalletConfig error: $it") }
            )
    }

    override fun restoreWalletConfig(masterSeed: MasterSeed): Single<RestoreWalletResponse> =
        minervaApi.getWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey))
            .map {
                if (it.state != ResponseState.ERROR) {
                    keystoreRepository.encryptMasterSeed(masterSeed)
                    localWalletProvider.saveWalletConfig(it.walletPayload)
                }
                RestoreWalletResponse(it.state, it.message)
            }

    override fun createWalletConfig(masterSeed: MasterSeed): Completable =
        minervaApi.saveWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey), walletConfigPayload = DefaultWalletConfig.create)
            .doOnComplete { localStorage.isSynced = true }
            .doOnError { localStorage.isSynced = false }
            .doOnTerminate {
                keystoreRepository.encryptMasterSeed(masterSeed)
                localWalletProvider.saveWalletConfig()
                initWalletConfig()
            }

    override fun updateWalletConfig(walletConfig: WalletConfig): Completable {
        return if (localStorage.isBackupAllowed) {
            val (config, payload) = Pair(walletConfig, WalletConfigToWalletPayloadMapper.map(walletConfig))
            minervaApi.saveWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey), walletConfigPayload = payload)
                .toSingleDefault(Pair(walletConfig, payload))
                .map {
                    localStorage.isSynced = true
                    it
                }
                .handleAutomaticBackupFailedError(Pair(config, payload), localStorage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { (config, payload) ->
                    _walletConfigLiveData.value = config
                    localWalletProvider.saveWalletConfig(payload)
                }.ignoreElement()
        } else {
            Completable.error(AutomaticBackupFailedThrowable())
        }
    }

    override fun dispose() {
        disposable?.dispose()
    }

    override fun getSafeAccountNumber(ownerAddress: String): Int {
        var safeAccountNumber = DEFAULT_SAFE_ACCOUNT_NUMBER
        getWalletConfig()?.accounts?.let {
            it.forEach { savedValue ->
                if (savedValue.masterOwnerAddress == ownerAddress) safeAccountNumber++
            }
        }
        return safeAccountNumber
    }

    override fun getSafeAccountMasterOwnerPrivateKey(address: String?): String {
        getWalletConfig()?.accounts?.forEach {
            if (it.address == address) return it.privateKey
        }
        return String.Empty
    }

    override fun updateSafeAccountOwners(position: Int, owners: List<String>): Single<List<String>> {
        getWalletConfig()?.let { config ->
            config.accounts.forEach { if (it.index == position) it.owners = owners }
            return updateWalletConfig(config.copy(version = config.updateVersion, accounts = config.accounts))
                .andThen(Single.just(owners))
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>> {
        getWalletConfig()?.let { TODO("Not yet implemented") }
        return Single.error(NotInitializedWalletConfigThrowable())
    }

    override fun getValueIterator(): Int {
        getWalletConfig()?.accounts?.let {
            var iterator = 1
            it.forEach { value -> if (!value.isSafeAccount) iterator += 1 }
            return iterator
        }
        return Int.InvalidValue
    }

    override fun getLoggedInIdentityByPublicKey(publicKey: String): Identity? {
        getWalletConfig()?.identities?.find { it.publicKey == publicKey }
            ?.let { return it }
            .orElse { return null }
    }

    override fun saveService(service: Service): Completable {
        getWalletConfig()?.run {
            if (services.isEmpty()) {
                return updateWalletConfig(this.copy(version = updateVersion, services = listOf(service)))
            }
            return updateWalletConfig(getWalletConfigWithUpdatedService(service))
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun getAccount(accountIndex: Int): Account? {
        getWalletConfig()?.accounts?.forEach {
            if (it.index == accountIndex) return it
        }
        return null
    }

    override fun findIdentityByDid(did: String): Identity? =
        getWalletConfig()?.let { config -> config.identities.find { it.did == did } }

    private fun fetchWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig> =
        if (localStorage.isBackupAllowed) {
            Observable.mergeDelayError(
                localWalletProvider.getWalletConfig()
                    .toObservable()
                    .doOnNext { localWalletConfigVersion = it.version }
                    .flatMap { completeKeys(masterSeed, it) },
                minervaApi.getWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey))
                    .toObservable()
                    .flatMap { handleSync(it, masterSeed) }
            ).onErrorResumeNext { _: Observer<in WalletConfig> ->
                localStorage.isSynced = false
                getLocalWalletConfig(masterSeed)
            }
        } else {
            getLocalWalletConfig(masterSeed)
        }

    private fun handleSync(it: WalletConfigResponse, masterSeed: MasterSeed): Observable<WalletConfig> =
        when {
            shouldBlockBackup(it.walletPayload.version) -> {
                localStorage.isBackupAllowed = false
                getLocalWalletConfig(masterSeed)
            }
            shouldSync(it.walletPayload.version) -> syncWalletConfig(masterSeed)
            else -> {
                localStorage.isSynced = true
                getLocalWalletConfig(masterSeed)
            }
        }

    private fun syncWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig> =
        minervaApi.saveWalletConfig(encodePublicKey(masterSeed.publicKey), localWalletProvider.getWalletConfig().blockingGet())
            .andThen { localStorage.isSynced = true }
            .toObservable<WalletConfig>()
            .onErrorResumeNext { _: Observer<in WalletConfig> ->
                localStorage.isSynced = false
                getLocalWalletConfig(masterSeed)
            }

    private fun shouldSync(serverVersion: Int) =
        serverVersion < localWalletConfigVersion

    private fun shouldBlockBackup(serverVersion: Int) =
        serverVersion > localWalletConfigVersion

    private fun getLocalWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig> =
        localWalletProvider.getWalletConfig()
            .toObservable()
            .flatMap { completeKeys(masterSeed, it) }

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

    private fun WalletConfig.getWalletConfigWithUpdatedService(newService: Service): WalletConfig {
        isServiceIsAlreadyConnected(newService)?.let {
            updateService(it)
            return WalletConfig(updateVersion, identities, accounts, services, credentials)
        }.orElse {
            return WalletConfig(updateVersion, identities, accounts, services + newService, credentials)
        }
    }

    private fun WalletConfig.isServiceIsAlreadyConnected(newService: Service) =
        services.find { service -> service.issuer == newService.issuer }

    private fun WalletConfig.updateService(service: Service) {
        services.forEach { if (it == service) it.lastUsed = DateUtils.timestamp }
    }

    companion object {
        private const val DEFAULT_SAFE_ACCOUNT_NUMBER = 1
        private const val START = 0
    }
}