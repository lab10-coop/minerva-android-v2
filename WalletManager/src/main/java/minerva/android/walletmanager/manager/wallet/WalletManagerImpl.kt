package minerva.android.walletmanager.manager.wallet

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.exchangemarketsprovider.api.BinanceApi
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.defs.Operation
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.configProvider.model.walletConfig.WalletConfigResponse
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.TokenPayload
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.manager.assets.AssetManager
import minerva.android.walletmanager.manager.wallet.walletconfig.repository.WalletConfigRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.Markets
import minerva.android.walletmanager.model.defs.ResponseState
import minerva.android.walletmanager.model.mappers.*
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.ServiceType
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
    private val walletConfigMutableLiveData = MutableLiveData<WalletConfig>()
    override val walletConfigLiveData: LiveData<WalletConfig> get() = walletConfigMutableLiveData

    private var disposable: Disposable? = null

    @VisibleForTesting
    fun loadWalletConfig() {
        disposable = walletConfigRepository.loadWalletConfig(masterKey)
            .subscribeBy(
                onNext = { walletConfigMutableLiveData.value = it },
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
            walletConfigMutableLiveData.value?.values?.filter { !it.isDeleted }?.let { values ->
                return blockchainRepository.refreshBalances(getAddresses(values))
                    .zipWith(Observable.range(START, this.size)
                        .flatMapSingle { binanceApi.fetchExchangeRate(this[it]) }
                        .toList())
                    .map { calculateFiatBalances(it.first, values, it.second) }
            }
        }
        return Single.error(Throwable("Wallet Config was not initialized"))
    }

    override fun transferNativeCoin(network: String, transaction: Transaction): Completable {
        return blockchainRepository.transferNativeCoin(network, mapTransactionToTransactionPayload(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()
    }

    override fun transferERC20Token(network: String, transaction: Transaction): Completable =
        blockchainRepository.transferERC20Token(network, mapTransactionToTransactionPayload(transaction))
            .andThen(blockchainRepository.reverseResolveENS(transaction.receiverKey).onErrorReturn { String.Empty })
            .map { saveRecipient(it, transaction.receiverKey) }
            .ignoreElement()

    override fun loadRecipients(): List<Recipient> = localStorage.loadRecipients()

    override fun resolveENS(ensName: String): Single<String> = blockchainRepository.resolveENS(ensName)

    private fun saveRecipient(ensName: String, address: String) = localStorage.saveRecipient(Recipient(ensName, address))

    override fun getTransferCosts(network: String, assetIndex: Int): TransactionCost {
        val operation = if (assetIndex == Int.InvalidIndex) Operation.TRANSFER_NATIVE else Operation.TRANSFER_ERC20
        return mapTransactionCostPayloadToTransactionCost(blockchainRepository.getTransactionCosts(network, assetIndex, operation))
    }

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

    override fun createValue(network: Network, valueName: String, ownerAddress: String, contractAddress: String): Completable {
        walletConfigMutableLiveData.value?.let { config ->
            val newValue = Value(config.newIndex, name = valueName, network = network.short)
            return cryptographyRepository.computeDeliveredKeys(masterKey.privateKey, newValue.index)
                .map { createUpdatedWalletConfig(config, newValue, it, ownerAddress, contractAddress) }
                .flatMapCompletable { updateWalletConfig(it) }
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    //TODO errors need to be handled better (Own Throwable implementation?)
    override fun removeValue(index: Int): Completable {
        walletConfigMutableLiveData.value?.let { config ->
            val newValues = config.values.toMutableList()
            config.values.forEachIndexed { position, value ->
                if (value.index == index) {
                    when {
                        areFundsOnValue(value.balance, value.assets) ->
                            return Completable.error(Throwable("This address is not empty and can't be removed."))
                        isNotSafeAccountMasterOwner(config.values, value) ->
                            return Completable.error(Throwable("You can not remove this Safe Account"))
                        hasMoreOwners(value) ->
                            return Completable.error(Throwable("This Safe Account have more owners"))
                    }
                    newValues[position] = Value(value, true)
                    return updateWalletConfig(WalletConfig(config.updateVersion, config.identities, newValues, config.services))
                }
            }
            return Completable.error(Throwable("Missing value with this index"))
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    private fun isNotSafeAccountMasterOwner(values: List<Value>, value: Value): Boolean {
        value.owners?.let {
            values.forEach {
                if (it.address == value.masterOwnerAddress) return false
            }
            return true
        }
        return false
    }

    private fun hasMoreOwners(value: Value): Boolean {
        value.owners?.let { return it.size > 1 }
        return false
    }

    override fun updateSafeAccountOwners(index: Int, owners: List<String>): Single<List<String>> {
        walletConfigMutableLiveData.value?.let { config ->
            config.values.apply {
                forEach { if (it.index == index) it.owners = owners }
                return updateWalletConfig(WalletConfig(config.updateVersion, config.identities, this, config.services))
                    .andThen(Single.just(owners))
            }
        }
        return Single.error(Throwable("Wallet Config was not initialized"))
    }

    override fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>> {
        walletConfigMutableLiveData.value?.let {
            TODO("Not yet implemented")
        }
        return Single.error(Throwable("Wallet Config was not initialized"))
    }

    override fun saveIdentity(identity: Identity): Completable {
        walletConfigMutableLiveData.value?.let { config ->
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
        walletConfigMutableLiveData.value?.identities?.apply {
            return if (inBounds(position)) this[position]
            else Identity(getNewIndex(), prepareDefaultIdentityName(defaultName))
        }
        return Identity(getNewIndex(), prepareDefaultIdentityName(defaultName))
    }

    override fun removeIdentity(identity: Identity): Completable {
        walletConfigMutableLiveData.value?.let { walletConfig ->
            walletConfig.identities.let { identities ->
                return handleRemovingIdentity(identities, getPositionForIdentity(identity, walletConfig), identity)
            }
        }
        return Completable.error(Throwable("Wallet config was not initialized"))
    }

    private fun handleRemovingIdentity(identities: List<Identity>, currentPosition: Int, identity: Identity): Completable {
        if (!identities.inBounds(currentPosition)) return Completable.error(Throwable("Missing identity to remove"))
        if (isOnlyOneElement(identities)) return Completable.error(Throwable("You can not remove last identity"))
        return saveIdentity(Identity(identity.index, identity.name, identity.publicKey, identity.privateKey, identity.data, true))
    }

    override fun decodeQrCodeResponse(token: String): Single<QrCodeResponse> =
        cryptographyRepository.decodeJwtToken(token)
            .map { mapHashMapToQrCodeResponse(it) }

    override fun decodePaymentRequestToken(token: String): Single<Pair<Payment, List<Service>?>> =
        cryptographyRepository.decodeJwtToken(token)
            .map { PaymentMapper.map(it) }
            .map { Pair(it, walletConfigLiveData.value?.services) }

    override suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String =
        cryptographyRepository.createJwtToken(payload, privateKey)

    override fun painlessLogin(url: String, jwtToken: String, identity: Identity, service: Service): Completable =
        servicesApi.painlessLogin(url = url, tokenPayload = TokenPayload(jwtToken))
            .flatMapCompletable {
                if (identity !is IncognitoIdentity) saveService(service)
                else Completable.complete()
            }

    override fun saveService(newService: Service): Completable {
        walletConfigMutableLiveData.value?.run {
            if (services.isEmpty()) {
                return updateWalletConfig(WalletConfig(updateVersion, identities, values, listOf(newService)))
            }
            return updateWalletConfig(getWalletConfigWithUpdatedService(newService))
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    private fun WalletConfig.getWalletConfigWithUpdatedService(newService: Service): WalletConfig {
        isServiceIsAlreadyConnected(newService)?.let {
            updateService(it)
            return WalletConfig(updateVersion, identities, values, services)
        }.orElse {
            return WalletConfig(updateVersion, identities, values, services + newService)
        }
    }

    private fun WalletConfig.isServiceIsAlreadyConnected(newService: Service) =
        services.find { service -> service.type == newService.type }

    private fun WalletConfig.updateService(service: Service) {
        services.forEach {
            if (it == service) {
                it.lastUsed = getLastUsedFormatted()
            }
        }
    }

    override fun getValueIterator(): Int {
        walletConfigMutableLiveData.value?.values?.size?.let { return it + 1 }
        throw Throwable("Wallet Config was not initialized")
    }

    override fun getSafeAccountNumber(ownerAddress: String): Int {
        var safeAccountNumber = DEFAULT_SAFE_ACCOUNT_NUMBER
        walletConfigLiveData.value?.values?.let {
            it.forEach { savedValue ->
                if (savedValue.masterOwnerAddress == ownerAddress) safeAccountNumber++
            }
        }
        return safeAccountNumber
    }

    override fun refreshAssetBalance(): Single<Map<String, List<Asset>>> {
        walletConfigMutableLiveData.value?.values?.let { values ->
            return Observable.range(START, values.size)
                .filter { position -> !values[position].isDeleted }
                //TODO filter should be removed when all testnet will be implemented
                .filter { position -> Network.fromString(values[position].network).run { this == Network.ETHEREUM || this == Network.ARTIS } }
                .flatMapSingle { position ->
                    AssetManager.getAssetAddresses(Network.fromString(values[position].network)).run {
                        refreshAssetsBalance(values[position], this)
                    }
                }
                .toList()
                .map { list -> list.map { it.first to AssetManager.mapToAssets(it.second) }.toMap() }
        }
        return Single.error(Throwable("Wallet Config was not initialized"))
    }


    /**
     *
     * return statement: Single<Pair<String, List<Pair<String, BigDecimal>>>>
     *                   Single<Pair<ValuePrivateKey, List<ContractAddress, BalanceOnContract>>>>
     *
     */
    private fun refreshAssetsBalance(value: Value, addresses: Pair<String, List<String>>):
            Single<Pair<String, List<Pair<String, BigDecimal>>>> =
        Observable.range(START, addresses.second.size)
            .flatMap { position ->
                blockchainRepository.refreshAssetBalance(value.privateKey, addresses.first, addresses.second[position], value.address)
            }
            .filter { it.second > NO_FUNDS }
            .toList()
            .map { Pair(value.privateKey, it) }

    private fun prepareNewValue(
        newValue: Value,
        keys: Triple<Int, String, String>,
        ownerAddress: String,
        contractAddress: String
    ) {
        newValue.apply {
            publicKey = keys.second
            privateKey = keys.third
            if (ownerAddress.isNotEmpty()) owners = mutableListOf(ownerAddress)
            address = if (contractAddress.isNotEmpty()) {
                this.contractAddress = contractAddress
                contractAddress
            } else blockchainRepository.completeAddress(keys.third)
        }
    }

    private fun handleWalletConfigResponse(it: WalletConfigResponse, masterKey: MasterKey) {
        if (it.state != ResponseState.ERROR) {
            keystoreRepository.encryptKey(masterKey)
            walletConfigRepository.saveWalletConfigLocally(it.walletPayload)
        }
    }

    private fun areFundsOnValue(balance: BigDecimal, assets: List<Asset>): Boolean {
        assets.forEach {
            if (blockchainRepository.toGwei(it.balance) >= MAX_GWEI_TO_REMOVE_VALUE) return true
        }
        return blockchainRepository.toGwei(balance) >= MAX_GWEI_TO_REMOVE_VALUE
    }

    private fun updateWalletConfig(walletConfig: WalletConfig): Completable {
        mapWalletConfigToWalletPayload(walletConfig).run {
            return walletConfigRepository.updateWalletConfig(masterKey, this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    walletConfigMutableLiveData.value = walletConfig
                    walletConfigRepository.saveWalletConfigLocally(this)
                }
                .doOnError {
                    //TODO Panic Button. Uncomment code below to save manually - not recommended was supported somewhere?
                    //walletConfigRepository.saveWalletConfigLocally(walletConfig)
                    //_walletConfigMutableLiveData.value = walletConfig
                }
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
        walletConfigMutableLiveData.value?.values?.apply {
            return if (inBounds(position)) this[position]
            else Value(Int.InvalidIndex)
        }
        Timber.e("Wallet Manager is not initialized!")
        return Value(Int.InvalidIndex)
    }

    private fun getNewIndex(): Int {
        walletConfigMutableLiveData.value?.let {
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

    private fun createUpdatedWalletConfig(
        config: WalletConfig,
        newValue: Value,
        keys: Triple<Int, String, String>,
        ownerAddress: String,
        contractAddress: String
    ): WalletConfig {
        prepareNewValue(newValue, keys, ownerAddress, contractAddress)
        config.run {
            val newValues = values.toMutableList()
            var newValuePosition = values.size
            values.forEachIndexed { position, value ->
                if (value.address == ownerAddress) newValuePosition = position + getSafeAccountNumber(ownerAddress)
            }
            newValues.add(newValuePosition, newValue)
            return WalletConfig(updateVersion, identities, newValues, services)
        }
    }

    override fun getSafeAccountMasterOwnerPrivateKey(address: String?): String {
        walletConfigLiveData.value?.values?.forEach { if (it.address == address) return it.privateKey }
        return String.Empty
    }

    override fun isAlreadyLoggedIn(issuer: String): Boolean {
        walletConfigMutableLiveData.value?.services?.forEach { if (doesChargingStationIsAlreadyLoggedIn(it, issuer)) return true }
        return false
    }

    override fun getLoggedInIdentityPublicKey(issuer: String): String {
        walletConfigMutableLiveData.value?.services?.find { it.type == issuer }?.let {
            return it.loggedInIdentityPublicKey
        }.orElse { return String.Empty }
    }

    override fun getLoggedInIdentity(publicKey: String): Identity? {
        walletConfigMutableLiveData.value?.identities?.find { it.publicKey == publicKey }?.let { return it }
            .orElse { return null }
    }

    private fun doesChargingStationIsAlreadyLoggedIn(service: Service, issuer: String) =
        service.type == issuer && service.type == ServiceType.CHARGING_STATION

    companion object {
        private const val START = 0
        private const val ONE_ELEMENT = 1
        private const val NEW_IDENTITY_TITLE_PATTERN = "%s #%d"
        private val MAX_GWEI_TO_REMOVE_VALUE = BigInteger.valueOf(300)
        private val NO_FUNDS = BigDecimal.valueOf(0)
        private const val DEFAULT_SAFE_ACCOUNT_NUMBER = 1
    }
}