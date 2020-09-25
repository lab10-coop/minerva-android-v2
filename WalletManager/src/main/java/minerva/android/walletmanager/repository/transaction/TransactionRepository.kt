package minerva.android.walletmanager.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.*
import java.math.BigDecimal
import java.math.BigInteger

interface TransactionRepository {
    fun refreshBalances(): Single<HashMap<String, Balance>>
    fun refreshAssetBalance(): Single<Map<String, List<AccountAsset>>>
    fun getTransferCosts(network: String, assetIndex: Int): TransactionCost
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal
    fun transferNativeCoin(network: String, accountIndex: Int, transaction: Transaction): Completable
    fun transferERC20Token(network: String, transaction: Transaction): Completable
    fun loadRecipients(): List<Recipient>
    fun resolveENS(ensName: String): Single<String>
    fun getAccount(accountIndex: Int): Account?

    fun subscribeToExecutedTransactions(accountIndex: Int): Flowable<PendingAccount>
    fun removePendingAccount(pendingAccount: PendingAccount)
    fun clearPendingAccounts()
    fun getPendingAccounts(): List<PendingAccount>
    fun getPendingAccount(accountIndex: Int): PendingAccount?
    fun getTransactions(): Single<List<PendingAccount>>
}