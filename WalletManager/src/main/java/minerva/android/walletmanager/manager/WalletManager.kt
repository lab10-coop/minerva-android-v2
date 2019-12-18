package minerva.android.walletmanager.manager

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxkotlin.subscribeBy
import minerva.android.configProvider.api.MinervaApi
import minerva.android.cryptographyProvider.repository.CryptographyRepository
import minerva.android.walletmanager.keystore.KeystoreRepository
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.walletconfig.WalletConfigRepository
import timber.log.Timber

interface WalletManager {
    val walletConfigLiveData: LiveData<WalletConfig>

    fun initWalletConfig(): Boolean
    fun isMasterKeyAvailable(): Boolean
    fun saveMasterKey(masterKey: MasterKey)
    fun validateMnemonic(mnemonic: String): List<String>
    fun saveWalletConfig(walletConfig: WalletConfig)
}

//TODO implement storing derivation path "m/99'/n" where n is index of identity and value
class WalletManagerImpl(
    private val keyStoreRepository: KeystoreRepository,
    private val cryptographyRepository: CryptographyRepository,
    private val walletConfigRepository: WalletConfigRepository,
    private val api: MinervaApi
) : WalletManager {

    private lateinit var masterKey: MasterKey


    //TODO refactor to Events
    private val _walletConfigLiveData = MutableLiveData<WalletConfig>()
    override val walletConfigLiveData: LiveData<WalletConfig> get() = _walletConfigLiveData

    override fun saveMasterKey(masterKey: MasterKey) = keyStoreRepository.encryptKey(masterKey)

    override fun validateMnemonic(mnemonic: String): List<String> = cryptographyRepository.validateMnemonic(mnemonic)

    override fun saveWalletConfig(walletConfig: WalletConfig) = walletConfigRepository.saveWalletConfig(walletConfig)

    override fun isMasterKeyAvailable(): Boolean = keyStoreRepository.isMasterKeySaved()

    override fun initWalletConfig(): Boolean =
        if (keyStoreRepository.isMasterKeySaved()) {
            masterKey = keyStoreRepository.decryptKey()
            loadWalletConfig()
            true
        } else false

    @VisibleForTesting
    fun loadWalletConfig() {
        walletConfigRepository.loadWalletConfig().subscribeBy(
            onNext = { _walletConfigLiveData.value = it },
            onError = { Timber.e("Downloading WalletConfig error: $it") },
            onComplete = { }
        )
    }
}