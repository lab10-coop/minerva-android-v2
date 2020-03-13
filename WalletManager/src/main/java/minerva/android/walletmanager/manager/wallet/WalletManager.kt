package minerva.android.walletmanager.manager.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.*
import java.math.BigDecimal
import java.math.BigInteger

//TODO divide WalletManager to: Service, Identity, Value, Transaction, Cryptography managers
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
    fun createValue(network: Network, valueName: String, ownerAddress: String = String.Empty, smartContractAddress: String = String.Empty): Completable
    fun removeValue(index: Int): Completable

    fun saveService(service: Service): Completable

    fun decodeQrCodeResponse(token: String): Single<QrCodeResponse>
    fun decodePaymentRequestToken(token: String): Single<Pair<Payment, List<Service>?>>
    suspend fun createJwtToken(payload: Map<String, Any?>, privateKey: String): String
    fun painlessLogin(url: String, jwtToken: String, identity: Identity): Completable

    fun refreshBalances(): Single<HashMap<String, Balance>>
    fun refreshAssetBalance(): Single<Map<String, List<Asset>>>
    fun transferNativeCoin(network: String, transaction: Transaction): Completable
    fun getTransactionCosts(network: String, assetIndex: Int): Single<TransactionCost>
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal

    fun transferERC20Token(network: String, transaction: Transaction): Completable
    fun loadRecipients(): List<Recipient>
    fun resolveENS(ensName: String): Single<String>

    fun getSafeAccountNumber(ownerPublicKey: String): Int
    fun getSafeAccountMasterOwnerPrivateKey(address: String?): String

    fun getValueIterator(): Int
    fun dispose()
}