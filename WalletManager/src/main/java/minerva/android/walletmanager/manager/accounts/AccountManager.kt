package minerva.android.walletmanager.manager.accounts

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.TokenVisibilitySettings
import minerva.android.walletmanager.model.wallet.MasterSeed

interface AccountManager : Manager {
    val masterSeed: MasterSeed

    fun loadAccount(index: Int): Account
    fun createRegularAccount(network: Network): Single<String>
    fun createSafeAccount(account: Account, contract: String): Completable
    fun removeAccount(account: Account): Completable
    fun getSafeAccountCount(ownerAddress: String): Int
    fun getSafeAccountName(account: Account): String
    fun isAddressValid(address: String): Boolean
    fun getTokenVisibilitySettings(): TokenVisibilitySettings
    fun saveFreeATSTimestamp()
    fun getLastFreeATSTimestamp(): Long
    fun saveTokenVisibilitySettings(settings: TokenVisibilitySettings): TokenVisibilitySettings
    fun currentTimeMills(): Long
    fun getAllAccounts(): List<Account>
    fun getAllActiveAccounts(chainId: Int): List<Account>
    fun toChecksumAddress(address: String): String
    fun clearFiat()
    val areMainNetworksEnabled: Boolean
    val isAuthenticationEnabled: Boolean
    var showMainNetworksWarning: Boolean
}