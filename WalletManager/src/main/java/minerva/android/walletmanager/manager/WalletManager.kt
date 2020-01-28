package minerva.android.walletmanager.manager

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.BlockchainProvider
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.servicesApiProvider.api.ServicesApi
import minerva.android.servicesApiProvider.model.TokenPayload
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.ServiceType
import minerva.android.walletmanager.utils.DateUtils
import minerva.android.walletmanager.walletconfig.Network
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

interface WalletManager {
    val walletConfigLiveData: LiveData<WalletConfig>
    val balanceLiveData: LiveData<HashMap<String, BigDecimal>>

    fun isMasterKeyAvailable(): Boolean
    fun initWalletConfig()
    fun validateMnemonic(mnemonic: String): List<String>
    fun createMasterKeys(callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun showMnemonic(callback: (error: Exception?, mnemonic: String) -> Unit)
    fun createWalletConfig(masterKey: MasterKey): Completable
    fun saveIsMnemonicRemembered()
    fun isMnemonicRemembered(): Boolean
    fun getWalletConfig(masterKey: MasterKey): Single<RestoreWalletResponse>
    fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun loadIdentity(position: Int, defaultName: String): Identity
    fun saveIdentity(identity: Identity): Completable
    fun createValue(network: Network, valueName: String): Completable
    fun removeIdentity(identity: Identity): Completable
    fun decodeJwtToken(jwtToken: String): Single<QrCodeResponse>
    fun computeDeliveredKeys(index: Int): Single<Triple<Int, String, String>>

    suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String
    fun painlessLogin(url: String, jwtToken: String, identity: Identity): Completable
    fun loadValue(position: Int): Value
    fun refreshBalances()
    fun getValueIterator(): Int
    fun sendTransaction(privateKey: String, receiverKey: String, amount: String, gasPrice: BigDecimal, gasLimit: BigInteger): Single<String>
    fun getTransactionCosts(): Single<Triple<BigDecimal, BigInteger, BigDecimal>>
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal
}

//Derivation path for identities and values "m/99'/n" where n is index of identity and value
class WalletManagerImpl(
    private val keystoreRepository: KeystoreRepository,
    private val cryptographyRepository: CryptographyRepository,
    private val walletConfigRepository: WalletConfigRepository,
    private val blockchainProvider: BlockchainProvider,
    private val localStorage: LocalStorage,
    private val servicesApi: ServicesApi
) : WalletManager {

    private lateinit var masterKey: MasterKey

    private val _walletConfigMutableLiveData = MutableLiveData<WalletConfig>()
    override val walletConfigLiveData: LiveData<WalletConfig> get() = _walletConfigMutableLiveData

    private val _balanceLiveData = MutableLiveData<HashMap<String, BigDecimal>>()
    override val balanceLiveData: LiveData<HashMap<String, BigDecimal>> get() = _balanceLiveData

    override fun isMasterKeyAvailable(): Boolean = keystoreRepository.isMasterKeySaved()

    override fun initWalletConfig() {
        masterKey = keystoreRepository.decryptKey()
        loadWalletConfig()
    }

    @VisibleForTesting
    fun loadWalletConfig() {
        walletConfigRepository.loadWalletConfig(masterKey).subscribeBy(
            onNext = { _walletConfigMutableLiveData.value = it },
            onError = { Timber.e("Downloading WalletConfig error: $it") },
            onComplete = { }
        )
    }

    override fun refreshBalances() {
        _walletConfigMutableLiveData.value?.values?.let { values ->
            val publicKeys = mutableListOf<String>()
            values.forEach { publicKeys.add(it.publicKey) }
            blockchainProvider.refreshBalances(publicKeys)
                .subscribeBy(
                    onSuccess = { onRefreshBalanceSuccess(it) },
                    onError = { Timber.e("Refresh balance error: ${it.message}") }
                )
        }
    }

    override fun sendTransaction(privateKey: String, receiverKey: String, amount: String, gasPrice: BigDecimal, gasLimit: BigInteger): Single<String> {
        return blockchainProvider.sendTransaction(privateKey, receiverKey, BigDecimal(amount), gasPrice, gasLimit)
    }

    override fun getTransactionCosts(): Single<Triple<BigDecimal, BigInteger, BigDecimal>> = blockchainProvider.getTransactionCosts()

    override fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal =
        blockchainProvider.calculateTransactionCost(gasPrice, gasLimit)

    override fun computeDeliveredKeys(index: Int): Single<Triple<Int, String, String>> =
        cryptographyRepository.computeDeliveredKeys(masterKey.privateKey, index)

    override fun getWalletConfig(masterKey: MasterKey): Single<RestoreWalletResponse> =
        walletConfigRepository.getWalletConfig(masterKey)
            .map {
                if (it.state != ResponseState.ERROR) {
                    keystoreRepository.encryptKey(masterKey)
                    walletConfigRepository.saveWalletConfigLocally(it.walletPayload)
                }
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
                    }
                    WalletConfig(config.updateVersion, config.identities, config.values + newValue)
                }.flatMapCompletable { updateWalletConfig(it) }
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
                    WalletConfig(config.updateVersion, prepareNewIdentitiesSet(identity, config), config.values)
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
            val currentPosition = getPositionForIdentity(identity, walletConfig)
            walletConfig.identities.let { identities ->
                //TODO handling error messages need to be designed and refactored
                if (!identities.inBounds(currentPosition)) return Completable.error(Throwable("Missing identity to remove"))
                if (isOnlyOneElement(identities)) return Completable.error(Throwable("You can not remove last identity"))
                return saveIdentity(Identity(identity.index, identity.name, identity.publicKey, identity.privateKey, identity.data, true))
            }
        }
        return Completable.error(Throwable("Wallet config was not initialized"))
    }

    override fun decodeJwtToken(jwtToken: String): Single<QrCodeResponse> =
        cryptographyRepository.decodeJwtToken(jwtToken)
            .map {
                if (it.isNotEmpty()) {
                    mapHashMapToQrCodeResponse(it)
                } else {
                    QrCodeResponse(isQrCodeValid = false)
                }
            }

    override suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String =
        cryptographyRepository.createJwtToken(payload, privateKey)

    override fun painlessLogin(url: String, jwtToken: String, identity: Identity): Completable =
        servicesApi.painlessLogin(url = url, tokenPayload = TokenPayload(jwtToken))
            .flatMapCompletable {
                handleSavingServiceLogin(identity)
            }

    private fun handleSavingServiceLogin(identity: Identity): CompletableSource? =
        if (identity !is IncognitoIdentity) saveService()
        else Completable.complete()

    override fun getValueIterator(): Int {
        _walletConfigMutableLiveData.value?.values?.size?.let {
            return it + 1
        }
        throw Throwable("Wallet Config was not initialized")
    }

    //    TODO chane for generic service creation, proper API is needed
    private fun saveService(): Completable {
        _walletConfigMutableLiveData.value?.let {
            return updateWalletConfig(
                WalletConfig(
                    it.updateVersion,
                    it.identities,
                    it.values,
                    listOf(Service(ServiceType.MINERVA, MINERVA_SERVICE, DateUtils.getLastUsed()))
                )
            )
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

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
        localStorage.isMenmonicRemembered()

    override fun loadValue(position: Int): Value {
        _walletConfigMutableLiveData.value?.values?.apply {
            return if (inBounds(position)) this[position]
            else Value(Int.InvalidIndex)
        }
        Timber.e("Wallet Manager is not initialized!")
        return Value(Int.InvalidIndex)
    }

    private fun onRefreshBalanceSuccess(list: List<Pair<String, BigDecimal>>) {
        val currentBalance = hashMapOf<String, BigDecimal>()
        list.forEach { balance ->
            currentBalance[balance.first] = balance.second
        }
        _balanceLiveData.value = currentBalance
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

    private fun prepareDefaultIdentityName(defaultName: String): String = String.format(NEW_IDENTITY_TITLE_PATTERN, defaultName, getNewIndex())

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
        private const val ONE_ELEMENT = 1
        private const val MINERVA_SERVICE = "Minerva Service"
        //        TODO should be dynamically handled form qr code
        private const val NEW_IDENTITY_TITLE_PATTERN = "%s #%d"
    }
}