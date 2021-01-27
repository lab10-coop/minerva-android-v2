package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.DappSession

interface DappSessionRepository {
    fun saveDappSession(dappSession: DappSession): Completable
    fun deleteDappSession(peerId: String): Completable
    fun getAllSessions(): Flowable<List<DappSession>>
    fun getConnectedDapps(): Single<List<DappSession>>
    fun deleteAllDappsForAccount(address: String): Completable
}