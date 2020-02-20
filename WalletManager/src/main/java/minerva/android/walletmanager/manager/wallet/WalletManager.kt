package minerva.android.walletmanager.manager.wallet

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.*
import java.math.BigDecimal
import java.math.BigInteger

//TODO divide WalletManager to: Service, Identity, Value, Transaction, Cryptography managers and add interfaces to providers and repositories
interface WalletManager {
    val walletConfigLiveData: LiveData<WalletConfig>
    val masterKey: MasterKey

    fun initWalletConfig()
    fun createWalletConfig(masterKey: MasterKey): Completable
    fun getWalletConfig(masterKey: MasterKey): Single<RestoreWalletResponse>

    fun isMasterKeyAvailable(): Boolean
    fun createMasterKeys(callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)
    fun restoreMasterKey(mnemonic: String, callback: (error: Exception?, privateKey: String, publicKey: String) -> Unit)

    fun validateMnemonic(mnemonic: String): List<String>
    fun showMnemonic(callback: (error: Exception?, mnemonic: String) -> Unit)
    fun saveIsMnemonicRemembered()
    fun isMnemonicRemembered(): Boolean

    fun loadIdentity(position: Int, defaultName: String): Identity
    fun saveIdentity(identity: Identity): Completable
    fun removeIdentity(identity: Identity): Completable

    fun loadValue(position: Int): Value
    fun createValue(network: Network, valueName: String): Completable
    fun removeValue(index: Int): Completable

    fun decodeJwtToken(jwtToken: String): Single<QrCodeResponse>
    suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String
    fun painlessLogin(url: String, jwtToken: String, identity: Identity): Completable

    fun refreshBalances(): Single<HashMap<String, Balance>>
    fun refreshAssetBalance(): Single<Map<String, List<Asset>>>
    fun sendTransaction(network: String, transaction: Transaction): Completable
    fun getTransactionCosts(network: String): Single<TransactionCost>
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal

    fun getValueIterator(): Int
    fun dispose()
}