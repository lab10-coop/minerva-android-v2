package minerva.android.walletmanager.manager.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import minerva.android.configProvider.localProvider.LocalWalletConfigProvider
import minerva.android.configProvider.model.walletConfig.AccountPayload
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
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
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.mappers.*
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.CryptoUtils.encodePublicKey
import minerva.android.walletmanager.utils.DefaultWalletConfig
import minerva.android.walletmanager.utils.handleAutomaticBackupFailedError
import timber.log.Timber
import kotlin.properties.Delegates

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
    private val _walletConfigLiveData = MutableLiveData<WalletConfig>()
    override val walletConfigLiveData: LiveData<WalletConfig> get() = _walletConfigLiveData

    private val _walletConfigErrorLiveData = MutableLiveData<Event<Throwable>>()
    override val walletConfigErrorLiveData: LiveData<Event<Throwable>> get() = _walletConfigErrorLiveData

    override fun getWalletConfig(): WalletConfig? = walletConfigLiveData.value
    override fun isMasterSeedSaved(): Boolean = keystoreRepository.isMasterSeedSaved()

    private var localWalletConfigVersion = Int.InvalidIndex

    override val isBackupAllowed: Boolean
        get() = localStorage.isBackupAllowed

    override val isSynced: Boolean
        get() = localStorage.isSynced

    override val areMainNetworksEnabled: Boolean
        get() = localStorage.areMainNetsEnabled

    private var enableMainNetsBehaviorSubject = BehaviorSubject.create<Boolean>()

    override var toggleMainNetsEnabled: Boolean? by Delegates.observable(localStorage.areMainNetsEnabled) { _, _: Boolean?, newValue: Boolean? ->
        newValue?.let {
            localStorage.areMainNetsEnabled = it
            enableMainNetsBehaviorSubject.onNext(it)
        }
    }
    override val enableMainNetsFlowable: Flowable<Boolean>
        get() = enableMainNetsBehaviorSubject.toFlowable(BackpressureStrategy.LATEST)
            .filter { toggleMainNetsEnabled != null }

    override fun getMnemonic(): String = cryptographyRepository.getMnemonicForMasterSeed(masterSeed.seed)


    override fun saveIsMnemonicRemembered() {
        localStorage.saveIsMnemonicRemembered(true)
    }

    override fun isMnemonicRemembered(): Boolean = localStorage.isMnemonicRemembered()

    override fun createWalletConfig(masterSeed: MasterSeed): Completable {

        Timber.tag("kobe").d("create wallet publicKey: ${encodePublicKey(masterSeed.publicKey)}")

        return minervaApi.saveWalletConfig(
            publicKey = encodePublicKey(masterSeed.publicKey),
            walletConfigPayload = DefaultWalletConfig.create
        )
            .doOnComplete { localStorage.isSynced = true }
            .doOnError { localStorage.isSynced = false }
            .doOnTerminate {
                this.masterSeed = masterSeed
                keystoreRepository.encryptMasterSeed(masterSeed)
                localWalletProvider.saveWalletConfig(DefaultWalletConfig.create)
                completeKeys(masterSeed, DefaultWalletConfig.create)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { _walletConfigLiveData.value = it }
            }
    }

    override fun initWalletConfig() {
        keystoreRepository.decryptMasterSeed()?.let {
            masterSeed = it
            loadWalletConfig(it)
        }.orElse {
            _walletConfigErrorLiveData.value = Event(Throwable())
        }
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

    override fun restoreWalletConfig(masterSeed: MasterSeed): Completable {

        Timber.tag("kobe").d("restore wallet publicKey: ${encodePublicKey(masterSeed.publicKey)}")

        return minervaApi.getWalletConfig(publicKey = encodePublicKey(masterSeed.publicKey))
            .map {
                keystoreRepository.encryptMasterSeed(masterSeed)
                localWalletProvider.saveWalletConfig(it)
            }.ignoreElement()
    }

    override fun updateWalletConfig(walletConfig: WalletConfig): Completable =
        if (localStorage.isBackupAllowed) {

            Timber.tag("kobe").d("update wallet publicKey: ${encodePublicKey(masterSeed.publicKey)}")


            val (config, payload) = Pair(
                walletConfig,
                WalletConfigToWalletPayloadMapper.map(walletConfig)
            )
            minervaApi.saveWalletConfig(
                publicKey = encodePublicKey(masterSeed.publicKey),
                walletConfigPayload = payload
            )
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
        } else Completable.error(AutomaticBackupFailedThrowable())

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

    override fun updateSafeAccountOwners(
        position: Int,
        owners: List<String>
    ): Single<List<String>> {
        getWalletConfig()?.let { config ->
            config.accounts.forEach { if (it.id == position) it.owners = owners }
            return updateWalletConfig(
                config.copy(
                    version = config.updateVersion,
                    accounts = config.accounts
                )
            )
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
                return updateWalletConfig(
                    this.copy(
                        version = updateVersion,
                        services = listOf(service)
                    )
                )
            }
            return updateWalletConfig(getWalletConfigWithUpdatedService(service))
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun getAccount(accountIndex: Int): Account? {
        getWalletConfig()?.accounts?.forEachIndexed { index, account ->
            if (index == accountIndex) return account
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
                minervaApi.getWalletConfigVersion(publicKey = encodePublicKey(masterSeed.publicKey))
                    .toObservable()
                    .flatMap { handleSync(it) }
            ).onErrorResumeNext { _: Observer<in WalletConfig> ->
                localStorage.isSynced = false
                getLocalWalletConfig(masterSeed)
            }
        } else {
            getLocalWalletConfig(masterSeed)
        }

    private fun handleSync(version: Int): Observable<WalletConfig> =
        when {
            shouldBlockBackup(version) -> {
                localStorage.isBackupAllowed = false
                getLocalWalletConfig(masterSeed)
            }
            shouldSync(version) -> syncWalletConfig(masterSeed)
            else -> {
                localStorage.isSynced = true
                getLocalWalletConfig(masterSeed)
            }
        }

    private fun syncWalletConfig(masterSeed: MasterSeed): Observable<WalletConfig> =
        minervaApi.saveWalletConfig(
            encodePublicKey(masterSeed.publicKey),
            localWalletProvider.getWalletConfig().blockingGet()
        )
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

    private fun completeKeys(
        masterSeed: MasterSeed,
        payload: WalletConfigPayload
    ): Observable<WalletConfig> {
        val identitiesResponse = payload.identityResponse
        val accountsResponse = payload.accountResponse
        return Observable.range(START, identitiesResponse.size)
            .filter { !identitiesResponse[it].isDeleted }
            .flatMapSingle {
                cryptographyRepository.calculateDerivedKeys(
                    masterSeed.seed,
                    identitiesResponse[it].index,
                    DID_PATH
                )
            }
            .toList()
            .map { keys -> completeIdentitiesKeys(payload, keys) }
            .map { completeIdentitiesProfileImages(it) }
            .zipWith(Observable.range(START, accountsResponse.size)
                .filter { !accountsResponse[it].isDeleted }
                .flatMapSingle {
                    cryptographyRepository.calculateDerivedKeys(
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
                        payload.erc20TokenResponse.map { (key, value) ->
                            key to value.map { ERC20TokenPayloadToERC20TokenMapper.map(it) }
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
            val address =
                if (accountPayload.contractAddress.isEmpty()) keys.address else accountPayload.contractAddress
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
        val isTestNet = NetworkManager.getNetwork(accountPayload.network).testNet
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
        NetworkManager.getNetwork(accountsResponse[index].network).testNet

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

    private fun WalletConfig.getWalletConfigWithUpdatedService(newService: Service): WalletConfig {
        var walletConfig = WalletConfig(updateVersion, identities, accounts, services, credentials, erc20Tokens)
        isServiceIsAlreadyConnected(newService)?.let {
            updateService(it)
        }.orElse {
            walletConfig = walletConfig.copy(services = services + newService)
        }
        return walletConfig
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