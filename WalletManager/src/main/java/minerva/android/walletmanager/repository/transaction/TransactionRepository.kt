package minerva.android.walletmanager.repository.transaction

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.model.minervaprimitives.account.*
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.wallet.MasterSeed
import java.math.BigDecimal
import java.math.BigInteger
import java.net.ConnectException

interface TransactionRepository {
    var newTokens: MutableList<ERCToken>
    val assetBalances: MutableList<AssetBalance>
    val masterSeed: MasterSeed
    fun getCoinBalance(): Flowable<Coin>
    fun getCoinBalanceByAccount(account: Account): Flowable<Coin>
    fun getTokenBalance(): Flowable<Asset>
    fun discoverNewTokens(): Single<Boolean>
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal
    fun transferNativeCoin(chainId: Int, accountIndex: Int, transaction: Transaction): Completable
    fun transferERC20Token(chainId: Int, transaction: Transaction): Completable
    fun transferERC721Token(chainId: Int, transaction: Transaction): Completable
    fun transferERC1155Token(chainId: Int, transaction: Transaction): Completable
    fun loadRecipients(): List<Recipient>
    fun resolveENS(ensName: String): Single<String>
    fun getAccount(accountIndex: Int): Account?
    fun getAccountByAddressAndChainId(address: String, chainId: Int): Account?
    fun getFreeATS(address: String): Completable
    fun getTokensRates(): Completable
    fun updateTokensRate()

    @Throws(ConnectException::class)
    fun subscribeToExecutedTransactions(accountIndex: Int): Flowable<PendingAccount>
    fun removePendingAccount(pendingAccount: PendingAccount)
    fun clearPendingAccounts()
    fun getPendingAccounts(): List<PendingAccount>
    fun getPendingAccount(accountIndex: Int): PendingAccount?
    fun getTransactions(): Single<List<PendingAccount>>
    fun getTransactionCosts(txCostPayload: TxCostPayload): Single<TransactionCost>

    fun isAddressValid(address: String, chainId: Int? = null): Boolean
    fun toRecipientChecksum(address: String, chainId: Int? = null): String
    fun shouldOpenNewWssConnection(accountIndex: Int): Boolean
    fun checkMissingTokensDetails(): Completable
    fun getCoinFiatRate(chainId: Int): Single<Double>
    fun getTokenFiatRate(tokenHash: String): Single<Double>
    fun toUserReadableFormat(value: BigDecimal): BigDecimal
    fun sendTransaction(chainId: Int, transaction: Transaction): Single<String>
    fun getFiatSymbol(): String
    fun isProtectTransactionEnabled(): Boolean
    fun getTokensUpdate(): Flowable<List<ERCToken>>
    fun updateTokens(): Completable

    fun getSuperTokenStreamInitBalance(): Flowable<Asset>
    fun startSuperTokenStreaming(chainId: Int): Flowable<Asset>
    val ratesMapLiveData: LiveData<Event<Unit>>

    fun getActiveAccounts(): List<Account>
    fun getActiveAccountsWithSuperfluidSupport(): List<Account>
}