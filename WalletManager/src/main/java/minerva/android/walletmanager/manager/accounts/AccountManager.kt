package minerva.android.walletmanager.manager.accounts

import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.wallet.MasterSeed

interface AccountManager : Manager {
    val masterSeed: MasterSeed
    val areMainNetworksEnabled: Boolean
    val isProtectKeysEnabled: Boolean
    val isProtectTransactionsEnabled: Boolean
    var hasAvailableAccounts: Boolean
    var activeAccounts: List<Account>
    var rawAccounts: List<Account>
    var cachedTokens: Map<Int, List<ERC20Token>>
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
    fun getAllFreeAccountForNetwork(chainId: Int): List<Pair<Int, String>>
    fun toChecksumAddress(address: String): String
    fun clearFiat()
    fun connectAccountToNetwork(index: Int, network: Network): Single<String>
    fun changeAccountName(existedAccount: Account, newName: String): Completable
    fun getFirstActiveAccountOrNull(chainId: Int): Account?
    fun getFirstActiveAccountForAllNetworks(): List<Account>
    fun createOrUnhideAccount(network: Network): Single<String>
}