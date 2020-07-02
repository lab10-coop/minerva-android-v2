package minerva.android.walletmanager.manager.values

import io.reactivex.Completable
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.Value

interface ValueManager : Manager {
    fun loadValue(position: Int): Value
    fun createValue(network: Network, valueName: String, ownerAddress: String = String.Empty, contract: String = String.Empty): Completable
    fun removeValue(index: Int): Completable
    fun getSafeAccountCount(ownerAddress: String): Int
}