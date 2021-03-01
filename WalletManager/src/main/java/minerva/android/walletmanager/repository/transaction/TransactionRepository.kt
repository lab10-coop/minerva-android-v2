package minerva.android.walletmanager.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.transactions.*
import minerva.android.walletmanager.model.wallet.MasterSeed
import java.math.BigDecimal
import java.math.BigInteger

interface TransactionRepository {
    val masterSeed: MasterSeed
    fun refreshBalances(): Single<HashMap<String, Balance>>

    /**
     * return statement: Map<AccountPrivateKey, List<AccountToken>>
     */
    fun refreshTokenBalance(): Single<Map<String, List<AccountToken>>>
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal
    fun transferNativeCoin(network: String, accountIndex: Int, transaction: Transaction): Completable
    fun transferERC20Token(network: String, transaction: Transaction): Completable
    fun loadRecipients(): List<Recipient>
    fun resolveENS(ensName: String): Single<String>
    fun getAccount(accountIndex: Int): Account?
    fun getAccountByAddress(address: String): Account?
    fun getFreeATS(address: String): Completable

    fun subscribeToExecutedTransactions(accountIndex: Int): Flowable<PendingAccount>
    fun removePendingAccount(pendingAccount: PendingAccount)
    fun clearPendingAccounts()
    fun getPendingAccounts(): List<PendingAccount>
    fun getPendingAccount(accountIndex: Int): PendingAccount?
    fun getTransactions(): Single<List<PendingAccount>>
    fun getTransactionCosts(txCostPayload: TxCostPayload): Single<TransactionCost>

    fun isAddressValid(address: String): Boolean
    fun shouldOpenNewWssConnection(accountIndex: Int): Boolean
    fun updateTokenIcons(): Completable
    fun getEurRate(chainId: Int): Single<Double>
    fun toEther(value: BigDecimal): BigDecimal
    fun sendTransaction(network: String, transaction: Transaction): Single<String>
}