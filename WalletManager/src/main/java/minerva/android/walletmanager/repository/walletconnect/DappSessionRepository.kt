package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.Completable
import io.reactivex.Flowable
import minerva.android.walletmanager.model.DappSession

interface DappSessionRepository {
    fun getConnectedDapps(addresses: String): Flowable<List<DappSession>>
    fun saveDappSession(dappSession: DappSession): Completable
    fun deleteDappSession(peerId: String): Completable
}