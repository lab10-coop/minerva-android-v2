package minerva.android.walletmanager.walletconfig

import com.google.gson.Gson
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
    private val online = OnlineMock()
    private val onlineLikeLocal = OnlineLikeLocalMock()
    private val api: MinervaApi = mock()

    private val repository = WalletConfigRepository(local, onlineLikeLocal, api)

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
//        val observable = repository.loadWalletConfig().delay(1, TimeUnit.SECONDS, testScheduler)
//        val testObserver = TestObserver<WalletConfig>()
//
//        observable.subscribe(testObserver)
//
//        testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS)
//        testObserver.assertNotTerminated()
//        testScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)
//        testObserver.assertValueSequence(
//            listOf(
//                Gson().fromJson(local.prepareData(), WalletConfig::class.java),
//                Gson().fromJson(online.prepareData(), WalletConfig::class.java)
//            )
//        )
//        testObserver.assertComplete()
//    }

    @Test
    fun `Check that WalletConfig will be updated when online version is the same`() {
        val observable = repository.loadWalletConfig().delay(1, TimeUnit.SECONDS, testScheduler)
        val testObserver = TestObserver<WalletConfig>()

        observable.subscribe(testObserver)

        testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS)
        testObserver.assertNotTerminated()
        testScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        testObserver.assertValue(Gson().fromJson(local.prepareData(), WalletConfig::class.java))
        testObserver.assertComplete()
    }

    @Test
    fun `create default walletConfig should return success`() {
        whenever(api.saveWalletConfig(any(), any(), any())).thenReturn(Completable.complete())
        val test = repository.createDefaultWalletConfig(MasterKey("1234", "5678")).test()
        test.assertNoErrors()
    }

    @Test
    fun `create default walletConfig should return error`() {
        val error = Throwable()
        whenever(api.saveWalletConfig(any(), any(), any())).thenReturn(Completable.error(error))
        val test = repository.createDefaultWalletConfig(MasterKey("1234", "5678")).test()
        test.assertError(error)
    }

    @Test
    fun `test slash encryption in public master key for http requests`() {
        val publicKey = repository.encodePublicKey(
            MasterKey(
                "BGQKOB5ZvopzLVObuzLtU/ujTMCvTU/CoX4A/DX5Ob1xH8RBAqwtpGoVZETWMMiyTuXtplSNVFeoeY6j8/uLCWA=",
                ""
            )
        )
        assertEquals(publicKey, "BGQKOB5ZvopzLVObuzLtU%2FujTMCvTU%2FCoX4A%2FDX5Ob1xH8RBAqwtpGoVZETWMMiyTuXtplSNVFeoeY6j8%2FuLCWA=")
    }
}
