package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.mappers.DappSessionToEntityMapper
import minerva.android.walletmanager.model.mappers.EntityToDappSessionMapper

class DappSessionRepositoryImpl(database: MinervaDatabase) : DappSessionRepository {

    private val dappDao = database.dappDao()

    override fun getConnectedDapps(addresses: String): Flowable<List<DappSession>> =
        dappDao.getConnectedDapps(addresses)
            .map { EntityToDappSessionMapper.map(it) }

    override fun saveDappSession(dappSession: DappSession): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getAllSessions(): Flowable<List<DappSession>> =
        dappDao.getAll().map { EntityToDappSessionMapper.map(it) }
}