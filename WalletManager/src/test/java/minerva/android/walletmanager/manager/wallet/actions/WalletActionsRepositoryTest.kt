package minerva.android.walletmanager.manager.wallet.actions

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.configProvider.api.MinervaApi
import minerva.android.configProvider.model.walletActions.WalletActionClusteredPayload
import minerva.android.configProvider.model.walletActions.WalletActionPayload
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.configProvider.model.walletActions.WalletActionsResponse
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepositoryImpl
import minerva.android.walletmanager.manager.walletActions.localProvider.LocalWalletActionsConfigProvider
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.WalletAction
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Test

class WalletActionsRepositoryTest {

    private val minervaApi: MinervaApi = mock()
    private val localWalletActionsConfigProvider: LocalWalletActionsConfigProvider = mock()
    private val repository = WalletActionsRepositoryImpl(minervaApi, localWalletActionsConfigProvider)
    private val actions = mutableListOf(WalletActionClusteredPayload(1L, mutableListOf(WalletActionPayload(1, 2, 1234L, hashMapOf()))))
    private val masterKey = MasterKey(_privateKey = "123", _publicKey = "456")
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
        whenever(minervaApi.getWalletActions(publicKey = "456")).thenReturn(
            Observable.just(WalletActionsResponse(_walletActionsConfigPayload = WalletActionsConfigPayload(1, actions)))
        )
        val test = repository.getWalletActions(MasterKey(_privateKey = "123", _publicKey = "456")).test()
        test.assertNoErrors()
        test.assertValue {
            it[0].walletActions[0].status == 2
        }
    }

    @Test
    fun `load wallet actions config error`() {
        whenever(localWalletActionsConfigProvider.loadWalletActionsConfig()).thenReturn(WalletActionsConfigPayload(1, actions))
        whenever(minervaApi.getWalletActions(publicKey = "456")).thenReturn(Observable.error(error))
        val test = repository.getWalletActions(masterKey).test()
        test.assertError(error)
    }

    @Test
    fun `save wallet actions success`() {
        whenever(localWalletActionsConfigProvider.loadWalletActionsConfig()).thenReturn(WalletActionsConfigPayload(1, actions))
        doNothing().whenever(localWalletActionsConfigProvider).saveWalletActionsConfig(WalletActionsConfigPayload(1, actions))
        whenever(minervaApi.saveWalletActions(any(), any(), any())).thenReturn(Completable.complete())
        val test = repository.saveWalletActions(WalletAction(1, 2, 1234L), masterKey).test()
        test.apply {
            assertNoErrors()
            assertComplete()
        }
    }

    @Test
    fun `save wallet actions error`() {
        whenever(localWalletActionsConfigProvider.loadWalletActionsConfig()).thenReturn(WalletActionsConfigPayload(1, actions))
        doNothing().whenever(localWalletActionsConfigProvider).saveWalletActionsConfig(WalletActionsConfigPayload(1, actions))
        whenever(minervaApi.saveWalletActions(any(), any(), any())).thenReturn(Completable.error(error))
        val test = repository.saveWalletActions(WalletAction(1, 2, 1234L), masterKey).test()
        test.assertError(error)
    }
}