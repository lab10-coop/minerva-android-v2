package minerva.android.walletmanager.repository

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Flowable
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.database.dao.DappSessionDao
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.repository.walletconnect.DappSessionRepositoryImpl
import minerva.android.walletmanager.utils.RxTest
import org.amshove.kluent.any
import org.junit.Test

class DappSessionRepositoryTest : RxTest() {

    private val dappSessionDao: DappSessionDao = mock()
    private val database: MinervaDatabase = mock {
        whenever(this.mock.dappDao()).thenReturn(dappSessionDao)
    }
    private val repository = DappSessionRepositoryImpl(database)

    private val dapps =
        listOf(DappSessionEntity(address = "address1"), DappSessionEntity("ddsdress2"))

    @Test
    fun `get connected dapps for account success test`() {
        whenever(dappSessionDao.getAll()).thenReturn(Flowable.just(dapps))
        repository.getSessions()
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it[0].address == "address1"
            }
    }

    @Test
    fun `get connected dapps for account error test`() {
        val error = Throwable()
        whenever(dappSessionDao.getAll()).thenReturn(Flowable.error(error))
        repository.getSessions()
            .test()
            .assertError(error)
    }

    @Test
    fun `save dapps success test`() {
        whenever(dappSessionDao.insert(any())).thenReturn(Completable.complete())
        repository.saveDappSession(DappSession(address = "address"))
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `save connected dapps error test`() {
        val error = Throwable()
        whenever(dappSessionDao.insert(any())).thenReturn(Completable.error(error))
        repository.saveDappSession(DappSession(address = "address"))
            .test()
            .assertError(error)
    }

    @Test
    fun `delete dapps success test`() {
        whenever(dappSessionDao.delete(any())).thenReturn(Completable.complete())
        repository.deleteDappSession("peerID")
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `delete connected dapps error test`() {
        val error = Throwable()
        whenever(dappSessionDao.delete(any())).thenReturn(Completable.error(error))
        repository.deleteDappSession("peerID")
            .test()
            .assertError(error)
    }

    @Test
    fun `get connected dapps success test`() {
        whenever(dappSessionDao.getAll()).thenReturn(Flowable.just(dapps))
        repository.getSessionsFlowable()
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it[0].address == "address1"
            }
    }

    @Test
    fun `get connected dapps error test`() {
        val error = Throwable()
        whenever(dappSessionDao.getAll()).thenReturn(Flowable.error(error))
        repository.getSessionsFlowable()
            .test()
            .assertError(error)
    }
}