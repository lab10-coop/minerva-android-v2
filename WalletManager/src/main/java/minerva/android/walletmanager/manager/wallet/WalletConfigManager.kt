package minerva.android.walletmanager.manager.wallet

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.*

interface WalletConfigManager {
    val walletConfigLiveData: LiveData<WalletConfig>
    val masterSeed: MasterSeed
    fun isMasterSeedSaved(): Boolean
    fun dispose()

    fun initWalletConfig()
    fun getWalletConfig(): WalletConfig?
    fun createWalletConfig(masterSeed: MasterSeed): Completable
    fun getWalletConfig(masterSeed: MasterSeed): Single<RestoreWalletResponse>
    fun updateWalletConfig(walletConfig: WalletConfig): Completable

    fun getSafeAccountNumber(ownerAddress: String): Int
    fun getSafeAccountMasterOwnerPrivateKey(address: String?): String
    fun updateSafeAccountOwners(position: Int, owners: List<String>): Single<List<String>>
    fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>>

    fun getValueIterator(): Int
    fun isAlreadyLoggedIn(issuer: String): Boolean
    fun getLoggedInIdentityPublicKey(issuer: String): String
    fun getLoggedInIdentity(publicKey: String): Identity?
    fun saveService(service: Service): Completable
    fun getValue(valueIndex: Int, assetIndex: Int): Value?

}