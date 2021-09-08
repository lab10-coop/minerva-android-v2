package minerva.android.walletmanager.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.repository.signature.SignatureRepository
import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.database.dao.DappSessionDao
import minerva.android.walletmanager.database.entity.DappSessionEntity
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepositoryImpl
import minerva.android.walletmanager.utils.logger.Logger
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals

class WalletConnectRepositoryTest {

    private val client: WCClient = mock()
    private val signatureRepository: SignatureRepository = mock()
    private val logger: Logger = mock()
    private lateinit var clientMap: ConcurrentHashMap<String, WCClient>
    private lateinit var repository: WalletConnectRepository

    private val dappSessionDao: DappSessionDao = mock()
    private val database: MinervaDatabase = mock {
        whenever(this.mock.dappDao()).thenReturn(dappSessionDao)
    }

    private val dapps =
        listOf(DappSessionEntity(address = "address1"), DappSessionEntity("ddsdress2"))


    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        clientMap = ConcurrentHashMap()
        clientMap["peerId"] = client
        repository = WalletConnectRepositoryImpl(signatureRepository, database, logger, client, clientMap).also {
            it.currentEthMessage =
                WCEthereumSignMessage(type = WCEthereumSignMessage.WCSignType.MESSAGE, raw = listOf("test1", "test2"))
        }
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `reject session test`() {
        whenever(clientMap["peerId"]?.rejectSession(any())).thenReturn(true)
        repository.rejectSession("peerId")
        assertEquals(clientMap.size, 0)
    }

    @Test
    fun `kill session test`() {
        whenever(clientMap["peerId"]?.killSession()).thenReturn(true)
        whenever(dappSessionDao.delete(any())).thenReturn(Completable.complete())
        repository.killSession("peerId")
        assertEquals(clientMap.size, 1)
    }

    @Test
    fun `reject request test`() {
        whenever(clientMap["peerId"]?.rejectRequest(any(), any())).thenReturn(true)
        repository.rejectRequest("peerId")
        verify(clientMap["peerId"])?.rejectRequest(any(), any())
    }

    @Test
    fun `accept request test`() {
        whenever(clientMap["peerId"]?.approveRequest(any(), eq(""))).thenReturn(true)
        whenever(signatureRepository.signData(any(), any())).thenReturn("privKey")
        repository.approveRequest("peerId", "privKey")
        verify(clientMap["peerId"])?.approveRequest(any(), eq("privKey"))
    }

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

    @Test
    fun `kill all account sessions test`() {
        whenever(dappSessionDao.getAll()).thenReturn(Flowable.just(dapps))
        whenever(dappSessionDao.deleteAllDappsForAccount(any())).thenReturn(Completable.complete())
        repository.killAllAccountSessions("address1", 1)
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `kill all account sessions error test`() {
        val error = Throwable()
        whenever(dappSessionDao.getAll()).thenReturn(Flowable.error(error))
        whenever(dappSessionDao.deleteAllDappsForAccount(any())).thenReturn(Completable.complete())
        repository.killAllAccountSessions("address1", 1)
            .test()
            .assertError(error)
    }

    @Test
    fun `get session by id test`() {
        whenever(dappSessionDao.getDappSessionById(any())).thenReturn(Single.just(DappSessionEntity(address = "address1")))
        repository.getDappSessionById("peerId")
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.address == "address1"
            }
    }

    @Test
    fun `get session by id error test`() {
        val error = Throwable()
        whenever(dappSessionDao.getDappSessionById(any())).thenReturn(Single.error(error))
        repository.getDappSessionById("peerId")
            .test()
            .assertError(error)
    }
}