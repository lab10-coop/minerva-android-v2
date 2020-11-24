package minerva.android.walletmanager.manager.accounts

import io.reactivex.Completable
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Network

interface AccountManager : Manager {
    fun loadAccount(position: Int): Account
    fun createRegularAccount(network: Network): Completable
    fun createSafeAccount(account: Account, contract: String): Completable
    fun removeAccount(index: Int): Completable
    fun getSafeAccountCount(ownerAddress: String): Int
    fun getSafeAccountName(account: Account): String
    fun isAddressValid(address: String): Boolean
}