package minerva.android.walletmanager.manager

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.kotlinUtils.list.inBounds
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import timber.log.Timber

interface WalletManager {
    val walletConfigLiveData: LiveData<WalletConfig>

    fun isMasterKeyAvailable(): Boolean
    fun initWalletConfig()
    fun validateMnemonic(mnemonic: String): List<String>
    fun createMasterKeys(callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun showMnemonic(callback: (error: Exception?, mnemonic: String) -> Unit)
    fun createDefaultWalletConfig(masterKey: MasterKey): Completable
    fun saveIsMnemonicRemembered()
    fun isMnemonicRemembered(): Boolean
    fun getWalletConfig(masterKey: MasterKey): Single<RestoreWalletResponse>
    fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun loadIdentity(position: Int, defaultName: String): Identity
    fun saveIdentity(identity: Identity): Completable
    fun removeIdentity(identity: Identity): Completable
}

//TODO derivation path for identities and values "m/99'/n" where n is index of identity and value
class WalletManagerImpl(
    private val keystoreRepository: KeystoreRepository,
    private val cryptographyRepository: CryptographyRepository,
    private val walletConfigRepository: WalletConfigRepository,
    private val localStorage: LocalStorage
) : WalletManager {

    private lateinit var masterKey: MasterKey

    private val _walletConfigMutableLiveData = MutableLiveData<WalletConfig>()
    override val walletConfigLiveData: LiveData<WalletConfig> get() = _walletConfigMutableLiveData

    override fun isMasterKeyAvailable(): Boolean = keystoreRepository.isMasterKeySaved()

    override fun initWalletConfig() {
        masterKey = keystoreRepository.decryptKey()
        loadWalletConfig()
    }

    @VisibleForTesting
    fun loadWalletConfig() {
        walletConfigRepository.loadWalletConfig(masterKey.publicKey).subscribeBy(
            onNext = { _walletConfigMutableLiveData.value = it },
            onError = { Timber.e("Downloading WalletConfig error: $it") },
            onComplete = { }
        )
    }

    override fun getWalletConfig(masterKey: MasterKey): Single<RestoreWalletResponse> =
        walletConfigRepository.getWalletConfig(masterKey)
            .map {
                if (it.state != ResponseState.ERROR) {
                    keystoreRepository.encryptKey(masterKey)
                    walletConfigRepository.saveWalletConfigLocally(mapWalletConfigResponseToWalletConfig(it))
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

    override fun createDefaultWalletConfig(masterKey: MasterKey): Completable {
        return walletConfigRepository.createDefaultWalletConfig(masterKey)
            .doOnComplete {
                keystoreRepository.encryptKey(masterKey)
                walletConfigRepository.saveWalletConfigLocally(walletConfigRepository.createDefaultWalletConfig())
            }
    }

    override fun saveIdentity(identity: Identity): Completable {
        _walletConfigMutableLiveData.value?.let {
            val walletConfig = WalletConfig(it.updateVersion, prepareNewIdentitiesSet(identity, it), it.values)
            return walletConfigRepository.updateWalletConfig(masterKey, walletConfig)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    walletConfigRepository.saveWalletConfigLocally(walletConfig)
                    _walletConfigMutableLiveData.value = walletConfig
                }
                //TODO Panic Button. Uncomment code below to save manually - not recommended was supported somewhere?
                .doOnError {
                    //                    walletConfigRepository.saveWalletConfigLocally(walletConfig)
                    //                    _walletConfigMutableLiveData.value = walletConfig
                }
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    override fun loadIdentity(position: Int, defaultName: String): Identity {
        _walletConfigMutableLiveData.value?.identities?.apply {
            return if (inBounds(position)) this[position]
            else Identity(walletConfigNewIndex(), prepareDefaultIdentityName(defaultName))
        }
        return Identity(walletConfigNewIndex(), prepareDefaultIdentityName(defaultName))
    }

    override fun removeIdentity(identity: Identity): Completable {
        _walletConfigMutableLiveData.value?.let { walletConfig ->
            val currentPosition = getPositionForIdentity(identity, walletConfig)
            walletConfig.identities?.let { identities ->
                //TODO handling error messages need to be designed and refactored
                if (!identities.inBounds(currentPosition)) return Completable.error(Throwable("Missing identity to remove"))
                if (isOnlyOneElement(identities)) return Completable.error(Throwable("You can not remove last identity"))
                return saveIdentity(Identity(identity.index, identity.name, identity.publicKey, identity.privateKey, identity.data, true))
            }
        }
        return Completable.error(Throwable("Wallet config was not initialized"))
    }

    override fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit) {
        cryptographyRepository.restoreMasterKey(mnemonic, callback)
    }

    override fun saveIsMnemonicRemembered() {
        localStorage.saveIsMnemonicRemembered(true)
    }

    override fun isMnemonicRemembered(): Boolean =
        localStorage.isMenmonicRemembered()


    private fun walletConfigNewIndex(): Int {
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

    private fun prepareDefaultIdentityName(defaultName: String): String = String.format("%s #%d", defaultName, walletConfigNewIndex())

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
    }
}