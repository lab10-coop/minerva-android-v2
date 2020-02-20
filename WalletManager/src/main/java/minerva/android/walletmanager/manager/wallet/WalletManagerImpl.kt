package minerva.android.walletmanager.manager.wallet

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.exchangemarketsprovider.api.BinanceApi
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.BlockchainRepository
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.TokenPayload
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.assets.AssetManager
import minerva.android.walletmanager.manager.wallet.walletconfig.repository.WalletConfigRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.defs.ResponseState
import minerva.android.walletmanager.model.mappers.mapHashMapToQrCodeResponse
import minerva.android.walletmanager.model.mappers.mapTransactionCostPayloadToTransactionCost
import minerva.android.walletmanager.model.mappers.mapTransactionToTransactionPayload
import minerva.android.walletmanager.model.mappers.mapWalletConfigToWalletPayload
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.ServiceType
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.utils.DateUtils.getLastUsedFormatted
import minerva.android.walletmanager.utils.MarketUtils.calculateFiatBalances
import minerva.android.walletmanager.utils.MarketUtils.getAddresses
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

//Derivation path for identities and values "m/99'/n" where n is index of identity and value
class WalletManagerImpl(
    private val keystoreRepository: KeystoreRepository,
    private val cryptographyRepository: CryptographyRepository,
    private val walletConfigRepository: WalletConfigRepository,
    private val blockchainRepository: BlockchainRepository,
    private val localStorage: LocalStorage,
    private val servicesApi: ServicesApi,
    private val binanceApi: BinanceApi
) : WalletManager {

    override lateinit var masterKey: MasterKey
    private val _walletConfigMutableLiveData = MutableLiveData<WalletConfig>()
    override val walletConfigLiveData: LiveData<WalletConfig> get() = _walletConfigMutableLiveData
    private var disposable: Disposable? = null

    @VisibleForTesting
    fun loadWalletConfig() {
        disposable = walletConfigRepository.loadWalletConfig(masterKey)
            .subscribeBy(
                onNext = { _walletConfigMutableLiveData.value = it },
                onError = { Timber.e("Downloading WalletConfig error: $it") }
            )
    }

    override fun dispose() {
        disposable?.dispose()
    }

    override fun isMasterKeyAvailable(): Boolean = keystoreRepository.isMasterKeySaved()

    override fun initWalletConfig() {
        masterKey = keystoreRepository.decryptKey()
        loadWalletConfig()
    }

    override fun refreshBalances(): Single<HashMap<String, Balance>> {
        listOf(Markets.ETH_EUR, Markets.POA_ETH).run {
            _walletConfigMutableLiveData.value?.values?.let { values ->
                return blockchainRepository.refreshBalances(getAddresses(values))
                    .zipWith(Observable.range(START, this.size)
                        .flatMapSingle { binanceApi.fetchExchangeRate(this[it]) }
                        .toList())
                    .map { calculateFiatBalances(it.first, values, it.second) }
            }
        }
        return Single.error(Throwable("Wallet Config was not initialized"))
    }

    override fun sendTransaction(network: String, transaction: Transaction): Completable =
        blockchainRepository.sendTransaction(network, mapTransactionToTransactionPayload(transaction))

    override fun getTransactionCosts(network: String): Single<TransactionCost> =
        blockchainRepository.getTransactionCosts(network)
            .map { mapTransactionCostPayloadToTransactionCost(it) }

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        blockchainRepository.calculateTransactionCost(gasPrice, gasLimit)

    override fun getWalletConfig(masterKey: MasterKey): Single<RestoreWalletResponse> =
        walletConfigRepository.getWalletConfig(masterKey)
            .map {
                handleWalletConfigResponse(it, masterKey)
                RestoreWalletResponse(it.state, it.message)
            }

    override fun validateMnemonic(mnemonic: String): List<String> = cryptographyRepository.validateMnemonic(mnemonic)

    override fun createMasterKeys(callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit) {
        cryptographyRepository.createMasterKey(callback)
    }

    override fun showMnemonic(callback: (error: Exception?, mnemonic: String) -> Unit) {
        cryptographyRepository.showMnemonicForMasterKey(keystoreRepository.decryptKey().privateKey, "TEST", callback)
    }

    override fun createWalletConfig(masterKey: MasterKey): Completable {
        return walletConfigRepository.createWalletConfig(masterKey)
            .doOnComplete {
                keystoreRepository.encryptKey(masterKey)
                walletConfigRepository.saveWalletConfigLocally(walletConfigRepository.createDefaultWalletConfig())
            }
    }

    override fun createValue(network: Network, valueName: String): Completable {
        _walletConfigMutableLiveData.value?.let { config ->
            val newValue = Value(config.newIndex, name = valueName, network = network.short)
            return cryptographyRepository.computeDeliveredKeys(masterKey.privateKey, newValue.index)
                .map {
                    newValue.apply {
                        publicKey = it.second
                        privateKey = it.third
                        address = blockchainRepository.completeAddress(it.third)
                    }
                    WalletConfig(config.updateVersion, config.identities, config.values + newValue, config.services)
                }.flatMapCompletable { updateWalletConfig(it) }
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    override fun removeValue(index: Int): Completable {
        _walletConfigMutableLiveData.value?.let { config ->
            val newValues = config.values.toMutableList()
            config.values.forEachIndexed { position, value ->
                if (value.index == index) {
                    if (isValueRemovable(value.balance)) {
                        //TODO need to be handled better (Own Throwable implementation?)
                        return Completable.error(Throwable("This address is not empty and can't be removed."))
                    }
                    newValues[position] = Value(value, true)
                    return updateWalletConfig(WalletConfig(config.updateVersion, config.identities, newValues, config.services))
                }
            }
            return Completable.error(Throwable("Missing value with this index"))
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    override fun saveIdentity(identity: Identity): Completable {
        _walletConfigMutableLiveData.value?.let { config ->
            return cryptographyRepository.computeDeliveredKeys(masterKey.privateKey, identity.index)
                .map {
                    identity.apply {
                        publicKey = it.second
                        privateKey = it.third
                    }
                    WalletConfig(config.updateVersion, prepareNewIdentitiesSet(identity, config), config.values, config.services)
                }.flatMapCompletable { updateWalletConfig(it) }
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    override fun loadIdentity(position: Int, defaultName: String): Identity {
        _walletConfigMutableLiveData.value?.identities?.apply {
            return if (inBounds(position)) this[position]
            else Identity(getNewIndex(), prepareDefaultIdentityName(defaultName))
        }
        return Identity(getNewIndex(), prepareDefaultIdentityName(defaultName))
    }

    override fun removeIdentity(identity: Identity): Completable {
        _walletConfigMutableLiveData.value?.let { walletConfig ->
            walletConfig.identities.let { identities ->
                return handleRemovingIdentity(identities, getPositionForIdentity(identity, walletConfig), identity)
            }
        }
        return Completable.error(Throwable("Wallet config was not initialized"))
    }

    override fun decodeJwtToken(jwtToken: String): Single<QrCodeResponse> =
        cryptographyRepository.decodeJwtToken(jwtToken)
            .map { mapHashMapToQrCodeResponse(it) }

    override suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String =
        cryptographyRepository.createJwtToken(payload, privateKey)

    override fun painlessLogin(url: String, jwtToken: String, identity: Identity): Completable =
        servicesApi.painlessLogin(url = url, tokenPayload = TokenPayload(jwtToken))
            .flatMapCompletable { handleSavingServiceLogin(identity) }

    private fun handleSavingServiceLogin(identity: Identity): CompletableSource? =
        if (identity !is IncognitoIdentity) saveService()
        else Completable.complete()

    //    TODO chane for generic service creation, proper API is needed
    private fun saveService(): Completable {
        _walletConfigMutableLiveData.value?.run {
            return updateWalletConfig(WalletConfig(updateVersion, identities, values, getMinervaService()))
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    override fun getValueIterator(): Int {
        _walletConfigMutableLiveData.value?.values?.size?.let {
            return it + 1
        }
        throw Throwable("Wallet Config was not initialized")
    }

    override fun refreshAssetBalance(): Single<Map<String, List<Asset>>> {
        _walletConfigMutableLiveData.value?.values?.let { values ->
            return Observable.range(START, values.size)
                .filter { position -> !values[position].isDeleted }
                //TODO filter should be removed when all testnet will be implemented
                .filter { position -> Network.fromString(values[position].network).run { this == Network.ETHEREUM || this == Network.ARTIS } }
                .flatMapSingle { position ->
                    refreshAssetsBalance(
                        values[position].privateKey,
                        AssetManager.getAssetAddresses(Network.fromString(values[position].network))
                    )
                }
                .toList()
                .map { list -> list.map { it.first to AssetManager.mapToAssets(it.second) }.toMap() }
        }
        return Single.error(Throwable("Wallet Config was not initialized"))
    }

    private fun refreshAssetsBalance(
        privateKey: String,
        addresses: Pair<String, List<String>>
    ): Single<Pair<String, List<Pair<String, BigDecimal>>>> =
        Observable.range(START, addresses.second.size)
            .flatMap { position -> blockchainRepository.refreshAssetBalance(privateKey, addresses.first, addresses.second[position]) }
            .filter { it.second > NO_FUNDS }
            .toList()
            .map { Pair(privateKey, it) }

    private fun handleWalletConfigResponse(it: WalletConfigResponse, masterKey: MasterKey) {
        if (it.state != ResponseState.ERROR) {
            keystoreRepository.encryptKey(masterKey)
            walletConfigRepository.saveWalletConfigLocally(it.walletPayload)
        }
    }

    private fun handleRemovingIdentity(identities: List<Identity>, currentPosition: Int, identity: Identity): Completable {
        if (!identities.inBounds(currentPosition)) return Completable.error(Throwable("Missing identity to remove"))
        if (isOnlyOneElement(identities)) return Completable.error(Throwable("You can not remove last identity"))
        return saveIdentity(Identity(identity.index, identity.name, identity.publicKey, identity.privateKey, identity.data, true))
    }

    private fun isValueRemovable(balance: BigDecimal) = blockchainRepository.toGwei(balance) >= MAX_GWEI_TO_REMOVE_VALUE

    private fun getMinervaService() =
        listOf(Service(ServiceType.MINERVA, MINERVA_SERVICE, getLastUsedFormatted()))

    private fun updateWalletConfig(walletConfig: WalletConfig): Completable {
        val walletConfigPayload = mapWalletConfigToWalletPayload(walletConfig)
        return walletConfigRepository.updateWalletConfig(masterKey, walletConfigPayload)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                walletConfigRepository.saveWalletConfigLocally(walletConfigPayload)
                _walletConfigMutableLiveData.value = walletConfig
            }
            //TODO Panic Button. Uncomment code below to save manually - not recommended was supported somewhere?
            .doOnError {
                //walletConfigRepository.saveWalletConfigLocally(walletConfig)
                //_walletConfigMutableLiveData.value = walletConfig
            }
    }

    override fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit) {
        cryptographyRepository.restoreMasterKey(mnemonic, callback)
    }

    override fun saveIsMnemonicRemembered() {
        localStorage.saveIsMnemonicRemembered(true)
    }

    override fun isMnemonicRemembered(): Boolean =
        localStorage.isMnemonicRemembered()

    override fun loadValue(position: Int): Value {
        _walletConfigMutableLiveData.value?.values?.apply {
            return if (inBounds(position)) this[position]
            else Value(Int.InvalidIndex)
        }
        Timber.e("Wallet Manager is not initialized!")
        return Value(Int.InvalidIndex)
    }

    private fun getNewIndex(): Int {
        _walletConfigMutableLiveData.value?.let {
            return it.newIndex
        }
        throw Throwable("Wallet Config was not initialized")
    }

    private fun prepareNewIdentitiesSet(identity: Identity, walletConfig: WalletConfig): List<Identity> {
        val newIdentities = walletConfig.identities.toMutableList()
        val position = getPositionForIdentity(identity, walletConfig)
        if (newIdentities.inBounds(position)) newIdentities[position] = identity
        else newIdentities.add(identity)
        return newIdentities
    }

    private fun prepareDefaultIdentityName(defaultName: String): String =
        String.format(NEW_IDENTITY_TITLE_PATTERN, defaultName, getNewIndex())

    private fun isOnlyOneElement(identities: List<Identity>): Boolean {
        var realIdentitiesCount = 0
        identities.forEach {
            if (!it.isDeleted) realIdentitiesCount++
        }
        return realIdentitiesCount <= ONE_ELEMENT
    }

    private fun getPositionForIdentity(newIdentity: Identity, walletConfig: WalletConfig): Int {
        walletConfig.identities.forEachIndexed { position, identity ->
            if (newIdentity.index == identity.index) return position
        }
        return walletConfig.identities.size
    }

    companion object {
        private const val START = 0
        private const val ONE_ELEMENT = 1
        private const val MINERVA_SERVICE = "Minerva Service"
        //        TODO should be dynamically handled form qr code
        private const val NEW_IDENTITY_TITLE_PATTERN = "%s #%d"
        private val MAX_GWEI_TO_REMOVE_VALUE = BigInteger.valueOf(300)
        private val NO_FUNDS = BigDecimal.valueOf(0)
    }
}