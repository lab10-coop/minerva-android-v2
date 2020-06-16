package minerva.android.walletmanager.manager.values

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig

interface ValueManager {
    val walletConfigLiveData: LiveData<WalletConfig>
    fun loadValue(position: Int): Value
    fun createValue(network: Network, valueName: String, ownerAddress: String = String.Empty, contract: String = String.Empty): Completable
    fun removeValue(index: Int): Completable
    fun getSafeAccountNumber(ownerAddress: String): Int
}