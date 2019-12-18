package minerva.android.walletmanager.walletconfig

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class WalletConfigRepositoryTest {

    private val testScheduler = TestScheduler()

    private val local = LocalMock()
    private val online = OnlineMock()
    private val onlineLikeLocal = OnlineLikeLocalMock()

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

    @Test
    fun `Check that WalletConfig will be updated when online version is different`() {
        val walletConfigRepository = WalletConfigRepository(local, online)
        val observable = walletConfigRepository.loadWalletConfig().delay(1, TimeUnit.SECONDS, testScheduler)
        val testObserver = TestObserver<WalletConfig>()

        observable.subscribe(testObserver)

        testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS)
        testObserver.assertNotTerminated()
        testScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        testObserver.assertValueSequence(
            listOf(
                Gson().fromJson(local.prepareData(), WalletConfig::class.java),
                Gson().fromJson(online.prepareData(), WalletConfig::class.java)
            )
        )
        testObserver.assertComplete()
    }

    @Test
    fun `Check that WalletConfig will be updated when online version is the same`() {
        val walletConfigRepository = WalletConfigRepository(local, onlineLikeLocal)

        val observable = walletConfigRepository.loadWalletConfig().delay(1, TimeUnit.SECONDS, testScheduler)
        val testObserver = TestObserver<WalletConfig>()

        observable.subscribe(testObserver)

        testScheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS)
        testObserver.assertNotTerminated()
        testScheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        testObserver.assertValue(Gson().fromJson(local.prepareData(), WalletConfig::class.java))
        testObserver.assertComplete()
    }
}
