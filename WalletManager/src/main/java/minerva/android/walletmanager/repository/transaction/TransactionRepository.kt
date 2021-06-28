package minerva.android.walletmanager.repository.transaction

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.CoinBalance
import minerva.android.walletmanager.model.minervaprimitives.account.PendingAccount
import minerva.android.walletmanager.model.minervaprimitives.account.TokenBalance
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.Recipient
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.model.transactions.TxCostPayload
import minerva.android.walletmanager.model.wallet.MasterSeed
import java.math.BigDecimal
import java.math.BigInteger

interface TransactionRepository {
    val masterSeed: MasterSeed
    fun refreshCoinBalances(): Single<List<CoinBalance>>

    /**
     * return statement: Map<AccountPrivateKey, List<AccountToken>>
     */
    fun refreshTokensBalances(): Single<List<TokenBalance>>
    fun discoverNewTokens(): Single<Boolean>
    fun calculateTransactionCost(gasPrice: BigDecimal, gasLimit: BigInteger): BigDecimal
    fun transferNativeCoin(chainId: Int, accountIndex: Int, transaction: Transaction): Completable
    fun transferERC20Token(chainId: Int, transaction: Transaction): Completable
    fun loadRecipients(): List<Recipient>
    fun resolveENS(ensName: String): Single<String>
    fun getAccount(accountIndex: Int): Account?
    fun getAccountByAddressAndChainId(address: String, chainId: Int): Account?
    fun getFreeATS(address: String): Completable
    fun getTokensRates(): Completable
    fun updateTokensRate()

    fun subscribeToExecutedTransactions(accountIndex: Int): Flowable<PendingAccount>
    fun removePendingAccount(pendingAccount: PendingAccount)
    fun clearPendingAccounts()
    fun getPendingAccounts(): List<PendingAccount>
    fun getPendingAccount(accountIndex: Int): PendingAccount?
    fun getTransactions(): Single<List<PendingAccount>>
    fun getTransactionCosts(txCostPayload: TxCostPayload): Single<TransactionCost>

    fun isAddressValid(address: String): Boolean
    fun shouldOpenNewWssConnection(accountIndex: Int): Boolean
    fun checkMissingTokensDetails(): Completable
    fun getCoinFiatRate(chainId: Int): Single<Double>
    fun getTokenFiatRate(tokenHash: String): Single<Double>
    fun toUserReadableFormat(value: BigDecimal): BigDecimal
    fun sendTransaction(chainId: Int, transaction: Transaction): Single<String>
    fun getFiatSymbol(): String
    fun isProtectTransactionEnabled(): Boolean
    fun getTaggedTokensUpdate(): Flowable<List<ERC20Token>>
}