package minerva.android.walletmanager.wallet

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.*
import java.math.BigDecimal
import java.math.BigInteger

//TODO divide WalletManager to: Service, Identity, Value, Transaction, Cryptography managers
interface WalletManager {
    val walletConfigLiveData: LiveData<WalletConfig>
    val masterSeed: MasterSeed

    fun initWalletConfig()
    fun createWalletConfig(masterSeed: MasterSeed): Completable
    fun getWalletConfig(masterSeed: MasterSeed): Single<RestoreWalletResponse>

    fun isMasterSeedAvailable(): Boolean
    fun createMasterSeed(): Single<MasterSeed>
    fun restoreMasterSeed(mnemonic: String): Single<MasterSeed>

    fun validateMnemonic(mnemonic: String): List<String>
    fun getMnemonic(): String
    fun saveIsMnemonicRemembered()
    fun isMnemonicRemembered(): Boolean

    fun loadIdentity(position: Int, defaultName: String): Identity
    fun saveIdentity(identity: Identity): Completable
    fun removeIdentity(identity: Identity): Completable

    fun loadValue(position: Int): Value
    fun createValue(
        network: Network, valueName: String, ownerAddress: String = String.Empty,
        contractAddress: String = String.Empty
    ): Completable

    fun removeValue(index: Int): Completable
    fun updateSafeAccountOwners(position: Int, owners: List<String>): Single<List<String>>
    fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>>

    fun saveService(service: Service): Completable

    fun decodeQrCodeResponse(token: String): Single<QrCodeResponse>
    fun decodePaymentRequestToken(token: String): Single<Pair<Payment, List<Service>?>>
    fun createJwtToken(payload: Map<String, Any?>, privateKey: String): Single<String>
    fun painlessLogin(url: String, jwtToken: String, identity: Identity, service: Service): Completable

    fun refreshBalances(): Single<HashMap<String, Balance>>
    fun refreshAssetBalance(): Single<Map<String, List<Asset>>>
    fun transferNativeCoin(network: String, transaction: Transaction): Completable
    fun getTransferCosts(network: String, assetIndex: Int): TransactionCost
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal

    fun transferERC20Token(network: String, transaction: Transaction): Completable
    fun loadRecipients(): List<Recipient>
    fun resolveENS(ensName: String): Single<String>

    fun getSafeAccountNumber(ownerAddress: String): Int
    fun getSafeAccountMasterOwnerPrivateKey(address: String?): String

    fun getValueIterator(): Int
    fun dispose()
    fun isAlreadyLoggedIn(issuer: String): Boolean
    fun getLoggedInIdentityPublicKey(issuer: String): String
    fun getLoggedInIdentity(publicKey: String): Identity?
}