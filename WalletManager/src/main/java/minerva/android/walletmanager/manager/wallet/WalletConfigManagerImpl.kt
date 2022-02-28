package minerva.android.walletmanager.manager.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.utils.CryptoUtils.encodePublicKey
import minerva.android.configProvider.localProvider.LocalWalletConfigProvider
import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.configProvider.repository.HttpNotFoundException
import minerva.android.configProvider.repository.MinervaApiRepository
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.cryptographyProvider.repository.model.DerivationPath.Companion.DID_PATH
import minerva.android.cryptographyProvider.repository.model.DerivationPath.Companion.MAIN_NET_PATH
import minerva.android.cryptographyProvider.repository.model.DerivationPath.Companion.TEST_NET_PATH
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
import minerva.android.walletmanager.exception.WalletConfigNotFoundThrowable
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.mappers.*
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.DefaultWalletConfig
import minerva.android.walletmanager.utils.handleAutomaticBackupFailedError
import timber.log.Timber
import java.math.BigDecimal

class WalletConfigManagerImpl(
    private val keystoreRepository: KeystoreRepository,
    private val cryptographyRepository: CryptographyRepository,
    private val localWalletProvider: LocalWalletConfigProvider,
    private val localStorage: LocalStorage,
    private val minervaApi: MinervaApiRepository
) : WalletConfigManager {

    override lateinit var masterSeed: MasterSeed
    private var disposable: Disposable? = null

    //TODO delete saving WalletConfig as reference - use Room for handling it - MinervaDatabase
    private val _walletConfigLiveData = MutableLiveData<Event<WalletConfig>>()
    override val walletConfigLiveData: LiveData<Event<WalletConfig>> get() = _walletConfigLiveData

    private val _walletConfigErrorLiveData = MutableLiveData<Event<Throwable>>()
    override val walletConfigErrorLiveData: LiveData<Event<Throwable>> get() = _walletConfigErrorLiveData

    override fun getWalletConfig(): WalletConfig =
        walletConfigLiveData.value?.peekContent() ?: throw NotInitializedWalletConfigThrowable()

    override fun isMasterSeedSaved(): Boolean = keystoreRepository.isMasterSeedSaved()

    private var localWalletConfigVersion = Int.InvalidIndex

    override val isBackupAllowed: Boolean
        get() = localStorage.isBackupAllowed

    override val isSynced: Boolean
        get() = localStorage.isSynced

    override var areMainNetworksEnabled: Boolean
        get() = localStorage.areMainNetworksEnabled
        set(value) {
            localStorage.areMainNetworksEnabled = value
        }

    override fun getMnemonic(): String = cryptographyRepository.getMnemonicForMasterSeed(masterSeed.seed)

    override fun saveIsMnemonicRemembered() {
        localStorage.saveIsMnemonicRemembered(true)
    }

    override fun isMnemonicRemembered(): Boolean = localStorage.isMnemonicRemembered()

    override fun createWalletConfig(masterSeed: MasterSeed): Completable =
        minervaApi.saveWalletConfig(encodePublicKey(masterSeed.publicKey), DefaultWalletConfig.create)
            .flatMap {
                it.saveWalletConfigToLocalStorageIfVersionChanged(DefaultWalletConfig.create.version)
            }
            .ignoreElement()
            .doOnComplete { localStorage.isSynced = true }
            .doOnError { localStorage.isSynced = false }
            .doOnTerminate {
                this.masterSeed = masterSeed
                keystoreRepository.encryptMasterSeed(masterSeed)
                localWalletProvider.saveWalletConfig(DefaultWalletConfig.create)
                disposable = completeKeys(masterSeed, DefaultWalletConfig.create)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onNext = { walletConfig ->
                            localWalletConfigVersion = walletConfig.version
                            _walletConfigLiveData.value = Event(walletConfig)
                        }, onError = { Timber.e(it) }
                    )
            }


    override fun restoreWalletConfig(masterSeed: MasterSeed): Completable =
        minervaApi.getWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey))
            .map { walletConfigPayload ->
                keystoreRepository.encryptMasterSeed(masterSeed)
                localWalletProvider.saveWalletConfig(walletConfigPayload)
            }
            .ignoreElement()
            .onErrorResumeNext { error -> Completable.error(getThrowableWhenRestoringWalletConfig(error)) }

    private fun getThrowableWhenRestoringWalletConfig(error: Throwable) =
        if (error is HttpNotFoundException) {
            WalletConfigNotFoundThrowable()
        } else {
            error
        }

    override fun initWalletConfig() {
        keystoreRepository.decryptMasterSeed()?.let { seed ->
            masterSeed = seed
            loadWalletConfig(seed)
        }.orElse {
            _walletConfigErrorLiveData.value = Event(Throwable())
        }
    }

    private fun loadWalletConfig(masterSeed: MasterSeed) {
        disposable = fetchWalletConfig(masterSeed)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { _walletConfigLiveData.value = Event(it) },
                onError = { Timber.e("Loading WalletConfig error: $it") }
            )
    }

    private fun fetchWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig> =
        if (localStorage.isBackupAllowed) {
            localWalletProvider.getWalletConfig()
                .toObservable()
                .zipWith(getWalletConfigVersion(masterSeed))
                .flatMap { (payload, serverVersion) -> handleSync(serverVersion, masterSeed, payload) }
        } else getLocalWalletConfig(masterSeed)

    private fun handleSync(serverVersion: Int, masterSeed: MasterSeed, payload: WalletConfigPayload) =
        if (serverVersion == Int.InvalidValue) {
            completeKeysWhenError(masterSeed, payload)
        } else {
            localWalletConfigVersion = payload.version
            compareVersions(serverVersion, payload)
        }

    private fun getWalletConfigVersion(masterSeed: MasterSeed) =
        minervaApi.getWalletConfigVersion(publicKey = encodePublicKey(masterSeed.publicKey))
            .toObservable()
            .onErrorReturn { Int.InvalidValue }

    private fun logVersionToFirebase(version: Int) {
        FirebaseCrashlytics.getInstance()
            .recordException(Throwable("PublicKey: ${masterSeed.publicKey} shouldBlockBackup($version)"))
    }

    private fun compareVersions(version: Int, payload: WalletConfigPayload): Observable<WalletConfig> =
        when {
            shouldBlockBackup(version) -> {
                logVersionToFirebase(version)
                localStorage.isBackupAllowed = false
                completeKeys(masterSeed, payload)
            }
            shouldSync(version) -> {
                syncWalletConfig(masterSeed, payload)
            }
            else -> {
                localStorage.isSynced = true
                completeKeys(masterSeed, payload)
            }
        }

    private fun getLocalWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig> =
        localWalletProvider.getWalletConfig()
            .toObservable()
            .flatMap { completeKeys(masterSeed, it) }

    private fun shouldSync(serverVersion: Int) =
        serverVersion < localWalletConfigVersion

    private fun shouldBlockBackup(serverVersion: Int) =
        serverVersion > localWalletConfigVersion

    private fun syncWalletConfig(masterSeed: MasterSeed, payload: WalletConfigPayload): Observable<WalletConfig> =
        minervaApi.saveWalletConfig(encodePublicKey(masterSeed.publicKey), payload)
            .flatMap {
                it.saveWalletConfigToLocalStorageIfVersionChanged(payload.version)
            }
            .ignoreElement()
            .andThen(completeKeys(masterSeed, payload))
            .map { walletConfig ->
                localStorage.isSynced = true
                walletConfig
            }

            .onErrorResumeNext { _: Observer<in WalletConfig> -> completeKeysWhenError(masterSeed, payload) }

    private fun completeKeysWhenError(masterSeed: MasterSeed, payload: WalletConfigPayload): Observable<WalletConfig> {
        localStorage.isSynced = false
        return completeKeys(masterSeed, payload)
    }

    override fun updateWalletConfig(walletConfig: WalletConfig): Completable =
        if (localStorage.isBackupAllowed) {
            localWalletProvider.saveWalletConfig(WalletConfigToWalletPayloadMapper.map(walletConfig))
                .observeOn(AndroidSchedulers.mainThread())
                .map { configPayload ->
                    _walletConfigLiveData.value = Event(walletConfig)
                    configPayload
                }
                .observeOn(Schedulers.io())
                .flatMap { walletConfigPayload ->
                    minervaApi.saveWalletConfig(encodePublicKey(masterSeed.publicKey), walletConfigPayload)
                }
                .flatMap {
                    it.saveWalletConfigToLocalStorageIfVersionChanged(walletConfig.version)
                }
                .map { localStorage.isSynced = true }
                .ignoreElement()
                .handleAutomaticBackupFailedError(localStorage) {
                    logVersionToFirebase(walletConfig.version)
                }
        } else Completable.error(AutomaticBackupFailedThrowable())

    override fun removeAllTokens(): Completable =
        updateWalletConfig(getWalletConfig().copy(erc20Tokens = emptyMap()))


    private fun WalletConfigPayload.saveWalletConfigToLocalStorageIfVersionChanged(oldVersion: Int) =
        if (version > oldVersion) {
            localWalletProvider.saveWalletConfig(this)
        } else Single.just(this)


    override fun dispose() {
        disposable?.dispose()
        disposable = null
    }

    override fun getSafeAccountNumber(ownerAddress: String): Int = getWalletConfig().accounts.let {
        var safeAccountNumber = DEFAULT_SAFE_ACCOUNT_NUMBER
        it.forEach { savedValue -> if (savedValue.masterOwnerAddress == ownerAddress) safeAccountNumber++ }
        safeAccountNumber
    }

    override fun getSafeAccountMasterOwnerPrivateKey(address: String?): String = getAccount(address).privateKey

    override fun getSafeAccountMasterOwnerBalance(address: String?): BigDecimal = getAccount(address).cryptoBalance

    private fun getAccount(address: String?): Account {
        getWalletConfig().accounts.forEach {
            if (it.address == address) return it
        }
        return Account(Int.InvalidIndex)
    }

    override fun updateSafeAccountOwners(
        position: Int,
        owners: List<String>
    ): Single<List<String>> = getWalletConfig().run {
        accounts.forEach { if (it.id == position) it.owners = owners }
        updateWalletConfig(copy(version = updateVersion, accounts = accounts))
            .andThen(Single.just(owners))
    }

    //TODO("Not yet implemented")
    override fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>> =
        Single.error(NotInitializedWalletConfigThrowable())

    override fun getValueIterator(): Int = getWalletConfig().accounts.let {
        var iterator = 1
        it.forEach { value -> if (!value.isSafeAccount) iterator += 1 }
        iterator
    }

    override fun getLoggedInIdentityByPublicKey(publicKey: String): Identity? =
        getWalletConfig().identities.find { it.publicKey == publicKey }

    override fun saveService(service: Service): Completable =
        getWalletConfig().run {
            val walletConfig = if (services.isEmpty()) {
                copy(version = updateVersion, services = listOf(service))
            } else {
                getWalletConfigWithUpdatedService(service, this)
            }
            updateWalletConfig(walletConfig)
        }

    private fun getWalletConfigWithUpdatedService(newService: Service, config: WalletConfig): WalletConfig =
        with(config) {
            var walletConfig = this.copy(version = updateVersion)
            walletConfig.services.find { service -> service.issuer == newService.issuer }?.let { service ->
                service.lastUsed = DateUtils.timestamp
            }.orElse {
                walletConfig = walletConfig.copy(services = services + newService)
            }
            return walletConfig
        }

    override fun getAccount(accountIndex: Int): Account? {
        getWalletConfig().accounts.forEachIndexed { index, account ->
            if (index == accountIndex) return account
        }
        return null
    }

    override fun findIdentityByDid(did: String): Identity? = getWalletConfig().run { identities.find { it.did == did } }

    private fun completeKeys(
        masterSeed: MasterSeed,
        payload: WalletConfigPayload
    ): Observable<WalletConfig> {
        val identitiesResponse = payload.identityResponse
        val accountsResponse = payload.accountResponse
        return Observable.fromIterable(identitiesResponse)
            .filter { identityPayload -> !identityPayload.isDeleted }
            .flatMapSingle { identityPayload ->
                cryptographyRepository.calculateDerivedKeysSingle(masterSeed.seed, identityPayload.index, DID_PATH)
            }
            .toList()
            .map { keys -> completeIdentitiesKeys(payload, keys) }
            .map { identities -> completeIdentitiesProfileImages(identities) }
            .zipWith(Observable.range(START, accountsResponse.size)
                .filter { !accountsResponse[it].isDeleted }
                .flatMapSingle {
                    cryptographyRepository.calculateDerivedKeysSingle(
                        masterSeed.seed,
                        accountsResponse[it].index,
                        getDerivationPath(accountsResponse, it),
                        isTestNet(accountsResponse, it)
                    )
                }
                .toList()
                .map { keys -> completeAccountsKeys(payload, keys) },
                BiFunction { identity: List<Identity>, account: List<Account> ->
                    WalletConfig(
                        payload.version,
                        identity,
                        account,
                        ServicesResponseToServicesMapper.map(payload.serviceResponse),
                        CredentialsPayloadToCredentials.map(payload.credentialResponse),
                        payload.erc20TokenResponse.map { (chainId, tokens) ->
                            chainId to tokens
                                .map { erC20TokenPayload -> ERC20TokenPayloadToERCTokenMapper.map(erC20TokenPayload) }
                        }.toMap()
                    )
                }
            ).toObservable()
    }

    private fun completeAccountsKeys(
        walletConfigPayload: WalletConfigPayload,
        derivedKeys: List<DerivedKeys>
    ): List<Account> {
        val accounts = mutableListOf<Account>()
        walletConfigPayload.accountResponse.forEach { accountPayload ->
            val keys = getAccountKeys(derivedKeys, accountPayload)
            val address = if (accountPayload.contractAddress.isEmpty()) keys.address else accountPayload.contractAddress
            accounts.add(
                AccountPayloadToAccountMapper.map(
                    accountPayload,
                    keys.publicKey,
                    keys.privateKey,
                    address
                )
            )
        }
        return accounts
    }

    private fun getAccountKeys(
        keys: List<DerivedKeys>,
        accountPayload: AccountPayload
    ): DerivedKeys {
        val isTestNet = accountPayload.isTestNetwork
        keys.forEach { derivedKeys ->
            if (derivedKeys.index == accountPayload.index && derivedKeys.isTestNet == isTestNet) {
                return derivedKeys
            }
        }
        return DerivedKeys(accountPayload.index, String.Empty, String.Empty, String.Empty)
    }

    private fun getDerivationPath(accountsResponse: List<AccountPayload>, index: Int) =
        if (isTestNet(accountsResponse, index)) {
            TEST_NET_PATH
        } else {
            MAIN_NET_PATH
        }

    private fun isTestNet(accountsResponse: List<AccountPayload>, index: Int) =
        accountsResponse[index].isTestNetwork

    private fun completeIdentitiesKeys(
        walletConfigPayload: WalletConfigPayload,
        keys: List<DerivedKeys>
    ): List<Identity> {
        val identities = mutableListOf<Identity>()
        walletConfigPayload.identityResponse.forEach { identityResponse ->
            getIdentityKeys(identityResponse.index, keys).let { key ->
                identities.add(
                    IdentityPayloadToIdentityMapper.map(
                        identityResponse,
                        key.publicKey,
                        key.privateKey,
                        key.address
                    )
                )
            }
        }
        return identities
    }

    private fun getIdentityKeys(index: Int, keys: List<DerivedKeys>): DerivedKeys {
        keys.forEach {
            if (it.index == index) return it
        }
        return DerivedKeys(index, String.Empty, String.Empty, String.Empty)
    }

    private fun completeIdentitiesProfileImages(identities: List<Identity>): List<Identity> {
        identities.forEach {
            it.profileImageBitmap =
                BitmapMapper.fromBase64(localStorage.getProfileImage(it.profileImageName))
        }
        return identities
    }

    companion object {
        private const val DEFAULT_SAFE_ACCOUNT_NUMBER = 1
        private const val START = 0
    }
}