package minerva.android.walletmanager.manager.accounts

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.AddressWrapper
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.CoinBalance
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.wallet.MasterSeed

interface AccountManager : Manager {
    val masterSeed: MasterSeed
    val areMainNetworksEnabled: Boolean
    val isChangeNetworkEnabled: Boolean
    val isProtectKeysEnabled: Boolean
    val isProtectTransactionsEnabled: Boolean
    var hasAvailableAccounts: Boolean
    var activeAccounts: List<Account>
    var rawAccounts: List<Account>
    var cachedTokens: Map<Int, List<ERCToken>>
    val getTokenVisibilitySettings: TokenVisibilitySettings
    fun areAllEmptyMainNetworkAccounts(): Boolean
    fun loadAccount(index: Int): Account
    fun createEmptyAccounts(numberOfAccounts: Int): Completable
    fun createSafeAccount(account: Account, contract: String): Completable
    fun removeAccount(account: Account): Completable
    fun hideAccount(account: Account): Completable
    fun getSafeAccountCount(ownerAddress: String): Int
    fun getSafeAccountName(account: Account): String
    fun isAddressValid(address: String): Boolean
    fun saveFreeATSTimestamp()
    fun getLastFreeATSTimestamp(): Long
    fun saveTokenVisibilitySettings(settings: TokenVisibilitySettings): TokenVisibilitySettings
    fun currentTimeMills(): Long
    fun getAllAccounts(): List<Account>
    fun getAllActiveAccounts(chainId: Int): List<Account>
    fun getNumberOfAccountsToUse(): Int
    fun getAllAccountsForSelectedNetworksType(): List<Account>
    fun getAllFreeAccountForNetwork(chainId: Int): List<AddressWrapper>
    fun toChecksumAddress(address: String, chainId: Int? = null): String
    fun clearFiat()
    fun connectAccountToNetwork(index: Int, network: Network): Single<String>
    fun changeAccountName(existedAccount: Account, newName: String): Completable
    fun getFirstActiveAccountOrNull(chainId: Int): Account?
    fun getFirstActiveAccountForAllNetworks(): List<Account>
    fun createOrUnhideAccount(network: Network): Single<String>
    fun insertCoinBalance(coinBalance: CoinBalance): Completable
    fun insertTokenBalance(coinBalance: CoinBalance, accountAddress: String): Completable
    fun getCachedCoinBalance(address: String, chainId: Int): Single<CoinBalance>
    fun getCachedTokenBalance(address: String, accountAddress: String): Single<CoinBalance>

    /**
    * Change Show Warning - change value for "showWarning" property (Account::showWarning)
    * @param existedAccount - instance of minerva.android.walletmanager.model.minervaprimitives.account.Accont
     * item which value will be changed
    * @param state - new state for Account::showWarning
    */
    fun changeShowWarning(existedAccount: Account, state: Boolean): Completable

    /**
     * Change Favorite State - change favorite state of selected nft
     * @param existedAccount - current Account
     * @param tokenId - id of token which must be changed
     * @param isFavoriteState - new state for token
     * @return Completable
     */
    fun changeFavoriteState(existedAccount: Account, tokenId: String, isFavoriteState: Boolean): Completable
    val balancesInsertLiveData: LiveData<Event<Unit>>
}