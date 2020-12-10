package minerva.android.walletmanager.manager.accounts

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Network

interface AccountManager : Manager {
    fun loadAccount(index: Int): Account
    fun createRegularAccount(network: Network): Single<String>
    fun createSafeAccount(account: Account, contract: String): Completable
    fun removeAccount(account: Account): Completable
    fun getSafeAccountCount(ownerAddress: String): Int
    fun getSafeAccountName(account: Account): String
    fun isAddressValid(address: String): Boolean

    val areMainNetworksEnabled: Boolean
    var toggleMainNetsEnabled: Boolean?
    val enableMainNetsFlowable: Flowable<Boolean>
}