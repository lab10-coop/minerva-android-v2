package minerva.android.walletmanager.manager.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.configProvider.repository.HttpBadRequestException
import minerva.android.kotlinUtils.DateUtils
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletmanager.exception.AutomaticBackupFailedThrowable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.ResponseState
import minerva.android.walletmanager.model.mappers.WalletConfigToWalletPayloadMapper
import minerva.android.walletmanager.utils.handleAutomaticBackupFailedError
import minerva.android.walletmanager.walletconfig.repository.WalletConfigRepository
import timber.log.Timber

class WalletConfigManagerImpl(
    private val keystoreRepository: KeystoreRepository,
    private val walletConfigRepository: WalletConfigRepository
) : WalletConfigManager {

    override lateinit var masterSeed: MasterSeed
    private var disposable: Disposable? = null

    private val _walletConfigLiveData = MutableLiveData<WalletConfig>()
    override val walletConfigLiveData: LiveData<WalletConfig> get() = _walletConfigLiveData

    private val _walletConfigErrorLiveData = MutableLiveData<Event<Throwable>>()
    override val walletConfigErrorLiveData: LiveData<Event<Throwable>> get() = _walletConfigErrorLiveData

    override fun getWalletConfig(): WalletConfig? = walletConfigLiveData.value
    override fun isMasterSeedSaved(): Boolean = keystoreRepository.isMasterSeedSaved()

    override fun initWalletConfig() {
        keystoreRepository.decryptMasterSeed()?.let {
            masterSeed = it
            loadWalletConfig(it)
        }.orElse { _walletConfigErrorLiveData.value = Event(Throwable()) }
    }

    private fun loadWalletConfig(masterSeed: MasterSeed) {
        disposable = walletConfigRepository.getWalletConfig(masterSeed)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { _walletConfigLiveData.value = it },
                onError = { Timber.e("Loading WalletConfig error: $it") }
            )
    }

    override fun createWalletConfig(masterSeed: MasterSeed): Completable =
        walletConfigRepository.updateWalletConfig(masterSeed)
            .doOnTerminate {
                keystoreRepository.encryptMasterSeed(masterSeed)
                walletConfigRepository.saveWalletConfigLocally()
                initWalletConfig()
            }

    override fun updateWalletConfig(walletConfig: WalletConfig): Completable {
        val pair = Pair(walletConfig, WalletConfigToWalletPayloadMapper.map(walletConfig))
        return walletConfigRepository.updateWalletConfig(masterSeed, pair.second)
            .toSingleDefault(Pair(walletConfig, pair.second))
            .handleAutomaticBackupFailedError(pair)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { (config, payload) ->
                _walletConfigLiveData.value = config
                walletConfigRepository.saveWalletConfigLocally(payload)
            }.ignoreElement()

    }

    override fun restoreWalletConfig(masterSeed: MasterSeed): Single<RestoreWalletResponse> =
        walletConfigRepository.restoreWalletConfig(masterSeed)
            .map {
                handleWalletConfigResponse(it, masterSeed)
                RestoreWalletResponse(it.state, it.message)
            }

    private fun handleWalletConfigResponse(walletConfigResponse: WalletConfigResponse, masterSeed: MasterSeed) {
        if (walletConfigResponse.state != ResponseState.ERROR) {
            keystoreRepository.encryptMasterSeed(masterSeed)
            walletConfigRepository.saveWalletConfigLocally(walletConfigResponse.walletPayload)
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

    override fun getAccount(accountIndex: Int): Account? {
        getWalletConfig()?.accounts?.forEach {
            if (it.index == accountIndex) return it
        }
        return null
    }

    override fun findIdentityByDid(did: String): Identity? =
        getWalletConfig()?.let { config -> config.identities.find { it.did == did } }

    companion object {
        private const val DEFAULT_SAFE_ACCOUNT_NUMBER = 1
    }
}