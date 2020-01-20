package minerva.android.walletmanager.walletconfig

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import minerva.android.configProvider.api.MinervaApi
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletConfig
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class WalletConfigRepositoryTest {

    private val testScheduler = TestScheduler()

    private val local = LocalMock()
    private val online: MinervaApi = OnlineMock()
    private val api: MinervaApi = mock()

    private val repository = WalletConfigRepository(local, online)

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

//    @Test
//    fun `Check that WalletConfig will be updated when online version is different`() {
//        val walletConfigRepository = WalletConfigRepository(local, online)
//        val observable = walletConfigRepository.loadWalletConfig("1234").delay(1, TimeUnit.SECONDS, testScheduler)
//        val testObserver = TestObserver<WalletConfig>()
//        val walletConfigResponse = (online as OnlineMock).prepareResponse()
//
//        observable.subscribe(testObserver)
//        testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS)
//        testObserver.assertNotTerminated()
//        testScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)
//        testObserver.assertValueSequence(
//            listOf(
//                local.prepareData(),
//                local.prepareData()
//            )
//        )
//        testObserver.assertComplete()
//    }

    @Test
    fun `Check that WalletConfig will be updated when online version is the same`() {
        val walletConfigRepository = WalletConfigRepository(local, OnlineLikeLocalMock())
        val observable = walletConfigRepository.loadWalletConfig("1234").delay(1, TimeUnit.SECONDS, testScheduler)
        val testObserver = TestObserver<WalletConfig>()

        observable.subscribe(testObserver)

        testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS)
        testObserver.assertNotTerminated()
        testScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        testObserver.assertValue(local.prepareData())
        testObserver.assertComplete()
    }

    @Test
    fun `create default walletConfig should return success`() {
        whenever(api.saveWalletConfig(any(), any(), any())).thenReturn(Completable.complete())
        val test = repository.createWalletConfig(MasterKey("1234", "5678")).test()
        test.assertNoErrors()
    }

    @Test
    fun `create default walletConfig should return error`() {
        val throwable = Throwable()
        val repository = WalletConfigRepository(local, api)
        whenever(api.saveWalletConfig(any(), any(), any())).thenReturn(Completable.error(throwable))
        val test = repository.createWalletConfig(MasterKey("1234", "5678")).test()
        test.assertError(throwable)
    }

    @Test
    fun `test slash encryption in public master key for http requests`() {
        val publicKey = repository.encodePublicKey(
            "BGQKOB5ZvopzLVObuzLtU/ujTMCvTU/CoX4A/DX5Ob1xH8RBAqwtpGoVZETWMMiyTuXtplSNVFeoeY6j8/uLCWA="
        )
        assertEquals(publicKey, "BGQKOB5ZvopzLVObuzLtU%2FujTMCvTU%2FCoX4A%2FDX5Ob1xH8RBAqwtpGoVZETWMMiyTuXtplSNVFeoeY6j8%2FuLCWA=")
    }
}
