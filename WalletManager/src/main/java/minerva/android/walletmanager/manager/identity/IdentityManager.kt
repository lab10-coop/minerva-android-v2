package minerva.android.walletmanager.manager.identity

import io.reactivex.Completable
import minerva.android.kotlinUtils.Empty
import minerva.android.walletmanager.manager.Manager
import minerva.android.walletmanager.model.Identity

interface IdentityManager : Manager {
    fun loadIdentity(position: Int, defaultName: String = String.Empty): Identity
    fun saveIdentity(identity: Identity): Completable
    fun removeIdentity(identity: Identity): Completable
}