package minerva.android.walletmanager.manager.identity

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.WalletConfig

interface IdentityManager {
    val walletConfigLiveData: LiveData<WalletConfig>
    fun loadIdentity(position: Int, defaultName: String): Identity
    fun saveIdentity(identity: Identity): Completable
    fun removeIdentity(identity: Identity): Completable
}