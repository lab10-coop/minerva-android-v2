package minerva.android.walletmanager.smartContract

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Token
import minerva.android.walletmanager.model.Transaction

interface SmartContractRepository {
    fun createSafeAccount(account: Account): Single<String>
    fun getSafeAccountOwners(contractAddress: String, network: String, privateKey: String, account: Account): Single<List<String>>
    fun addSafeAccountOwner(
        owner: String,
        address: String,
        network: String,
        privateKey: String,
        account: Account
    ): Single<List<String>>

    fun removeSafeAccountOwner(
        removeAddress: String, address: String,
        network: String, privateKey: String, account: Account
    ): Single<List<String>>

    fun transferNativeCoin(network: String, transaction: Transaction): Completable
    fun transferERC20Token(network: String, transaction: Transaction, erc20Address: String): Completable
    fun getSafeAccountMasterOwnerPrivateKey(address: String?): String
    fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>>
    fun getERC20TokenDetails(privateKey: String, network: String, tokenAddress: String): Single<Token>
}