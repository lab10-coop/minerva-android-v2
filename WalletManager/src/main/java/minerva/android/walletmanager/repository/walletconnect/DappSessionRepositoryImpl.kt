package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.mappers.DappSessionToEntityMapper
import minerva.android.walletmanager.model.mappers.EntityToDappSessionMapper
import timber.log.Timber

class DappSessionRepositoryImpl(minervaDatabase: MinervaDatabase) : DappSessionRepository {

    private val dappDao = minervaDatabase.dappDao()

    override fun saveDappSession(dappSession: DappSession): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getSessionsFlowable(): Flowable<List<DappSession>> =
        dappDao.getAll().map { EntityToDappSessionMapper.map(it) }

    override fun getSessions(): Single<List<DappSession>> =
        dappDao.getAll().firstOrError().map { EntityToDappSessionMapper.map(it) }

    override fun deleteAllDappsForAccount(address: String): Completable =
        dappDao.deleteAllDappsForAccount(address)
}