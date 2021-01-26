package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.database.dao.DappSessionDao
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.mappers.DappSessionToEntityMapper
import minerva.android.walletmanager.model.mappers.EntityToDappSessionMapper

class DappSessionRepositoryImpl(sessionDao: DappSessionDao) : DappSessionRepository {

    private val dappDao = sessionDao

    override fun getConnectedDapps(): Single<List<DappSession>> =
        dappDao.getAll().firstOrError().map { EntityToDappSessionMapper.map(it) }

    override fun saveDappSession(dappSession: DappSession): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getAllSessions(): Flowable<List<DappSession>> =
        dappDao.getAll().map { EntityToDappSessionMapper.map(it) }
}