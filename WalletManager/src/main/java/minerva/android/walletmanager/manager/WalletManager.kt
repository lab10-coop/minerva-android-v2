package minerva.android.walletmanager.manager

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import minerva.android.walletmanager.walletconfig.WalletConfigRepository.Companion.DEFAULT_IDENTITY_NAME
import minerva.android.walletmanager.walletconfig.WalletConfigRepository.Companion.DEFAULT_VERSION
import minerva.android.walletmanager.walletconfig.WalletConfigRepository.Companion.FIRST_IDENTITY_INDEX
import minerva.android.walletmanager.walletconfig.WalletConfigRepository.Companion.FIRST_VALUES_INDEX
import minerva.android.walletmanager.walletconfig.WalletConfigRepository.Companion.SECOND_VALUES_INDEX
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
}

//TODO derivation path for identities and values "m/99'/n" where n is index of identity and value
class WalletManagerImpl(
    private val keystoreRepository: KeystoreRepository,
    private val cryptographyRepository: CryptographyRepository,
    private val walletConfigRepository: WalletConfigRepository,
    private val localStorage: LocalStorage
) : WalletManager {

    private lateinit var masterKey: MasterKey

    //TODO refactor to Events
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
                walletConfigRepository.saveWalletConfigLocally(createDefaultWalletConfig())
            }
    }

    override fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit) {
        cryptographyRepository.restoreMasterKey(mnemonic, callback)
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

    override fun saveIsMnemonicRemembered() {
        localStorage.saveIsMnemonicRemembered(true)
    }

    override fun isMnemonicRemembered(): Boolean =
        localStorage.isMenmonicRemembered()

    private fun createDefaultWalletConfig() =
        WalletConfig(
            DEFAULT_VERSION, listOf(Identity(index = FIRST_IDENTITY_INDEX, name = DEFAULT_IDENTITY_NAME)),
            listOf(Value(index = FIRST_VALUES_INDEX), Value(index = SECOND_VALUES_INDEX))
        )
}