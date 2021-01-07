package minerva.android.walletmanager.walletactions

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.model.walletActions.WalletActionClusteredPayload
import minerva.android.configProvider.model.walletActions.WalletActionPayload
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.configProvider.repository.MinervaApiRepository
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.WalletAction
import minerva.android.walletmanager.walletActions.WalletActionsRepositoryImpl
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProvider
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Test

class WalletActionsRepositoryTest {

    private val minervaApi: MinervaApiRepository = mock()
    private val localWalletActionsConfigProvider: LocalWalletActionsConfigProvider = mock()
    private val walletConfigManager: WalletConfigManager = mock()
    private val repository = WalletActionsRepositoryImpl(minervaApi, localWalletActionsConfigProvider, walletConfigManager)
    private val actions =
        mutableListOf(WalletActionClusteredPayload(1L, mutableListOf(WalletActionPayload(1, 2, 1234L, hashMapOf()))))
    private val masterSeed = MasterSeed(_privateKey = "123", _publicKey = "456", _seed = "seed")
    private val error = Throwable()

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
    fun `load wallet actions config success`() {
        whenever(localWalletActionsConfigProvider.loadWalletActionsConfig()).thenReturn(WalletActionsConfigPayload(1, actions))
        whenever(walletConfigManager.masterSeed).thenReturn(masterSeed)
        whenever(minervaApi.getWalletActions(publicKey = "456")).thenReturn(
            Observable.just(WalletActionsResponse(_walletActionsConfigPayload = WalletActionsConfigPayload(1, actions)))
        )
        repository.getWalletActions()
            .test()
            .assertNoErrors()
            .assertValue {
                it[0].walletActions[0].status == 2
            }
    }

    @Test
    fun `load wallet actions when api error occurs`() {
        whenever(localWalletActionsConfigProvider.loadWalletActionsConfig()).thenReturn(WalletActionsConfigPayload(1, actions))
        whenever(walletConfigManager.masterSeed).thenReturn(masterSeed)
        whenever(minervaApi.getWalletActions(publicKey = "456")).thenReturn(Observable.error(error))
        repository.getWalletActions()
            .test()
            .assertValue {
                it[0].lastUsed == 1L
            }
    }

    @Test
    fun `save wallet actions success`() {
        whenever(localWalletActionsConfigProvider.loadWalletActionsConfig()).thenReturn(WalletActionsConfigPayload(1, actions))
        doNothing().whenever(localWalletActionsConfigProvider).saveWalletActionsConfig(WalletActionsConfigPayload(1, actions))
        whenever(minervaApi.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletConfigManager.masterSeed) doReturn masterSeed
        repository.saveWalletActions(listOf(WalletAction(1, 2, 1234L)))
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `save wallet actions error`() {
        whenever(localWalletActionsConfigProvider.loadWalletActionsConfig()).thenReturn(WalletActionsConfigPayload(1, actions))
        doNothing().whenever(localWalletActionsConfigProvider).saveWalletActionsConfig(WalletActionsConfigPayload(1, actions))
        whenever(minervaApi.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletConfigManager.masterSeed) doReturn masterSeed
        repository.saveWalletActions(listOf(WalletAction(1, 2, 1234L)))
            .test()
            .assertComplete()
    }
}