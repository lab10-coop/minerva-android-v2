package minerva.android.walletmanager.manager.accounts

import io.reactivex.Completable
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Network

interface AccountManager : Manager {
    fun loadAccount(position: Int): Account
    fun createAccount(network: Network, accountName: String, ownerAddress: String = String.Empty, contract: String = String.Empty): Completable
    fun removeAccount(index: Int): Completable
    fun getSafeAccountCount(ownerAddress: String): Int
}