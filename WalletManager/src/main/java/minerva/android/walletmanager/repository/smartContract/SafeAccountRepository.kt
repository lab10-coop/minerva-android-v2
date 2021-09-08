package minerva.android.walletmanager.repository.smartContract

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.Transaction
import java.math.BigDecimal

interface SafeAccountRepository {
    fun createSafeAccount(account: Account): Single<String>
    fun getSafeAccountOwners(contractAddress: String, chainId: Int, privateKey: String, account: Account): Single<List<String>>
    fun addSafeAccountOwner(
        owner: String,
        address: String,
        chainId: Int,
        privateKey: String,
        account: Account
    ): Single<List<String>>

    fun removeSafeAccountOwner(
        removeAddress: String, address: String,
        chainId: Int, privateKey: String, account: Account
    ): Single<List<String>>

    fun transferNativeCoin(chainId: Int, transaction: Transaction): Completable
    fun transferERC20Token(chainId: Int, transaction: Transaction, erc20Address: String): Completable
    fun getSafeAccountMasterOwnerPrivateKey(address: String?): String
    fun getSafeAccountMasterOwnerBalance(address: String?): BigDecimal
    fun removeSafeAccountOwner(index: Int, owner: String): Single<List<String>>
    fun getERC20TokenDetails(privateKey: String, chainId: Int, tokenAddress: String): Single<ERC20Token>
}