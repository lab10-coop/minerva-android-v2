package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.model.DappSession

interface DappSessionRepository {
    fun saveDappSession(dappSession: DappSession): Completable
    fun deleteDappSession(peerId: String): Completable
    fun getSessionsFlowable(): Flowable<List<DappSession>>
    fun getSessions(): Single<List<DappSession>>
    fun deleteAllDappsForAccount(address: String): Completable
}