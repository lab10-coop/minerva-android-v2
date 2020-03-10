package minerva.android.values

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.SmartContractManager
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.wallet.WalletManagerImpl
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.*
import org.amshove.kluent.shouldBe
import org.junit.Test
import java.math.BigDecimal

class ValuesViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val smartContractManager: SmartContractManager = mock()
    private val viewModel = ValuesViewModel(walletManager, walletActionsRepository, smartContractManager)

    private val balanceObserver: Observer<HashMap<String, Balance>> = mock()
    private val balanceCaptor: KArgumentCaptor<HashMap<String, Balance>> = argumentCaptor()

    private val assetsBalanceObserver: Observer<Map<String, List<Asset>>> = mock()
    private val assetsBalanceCaptor: KArgumentCaptor<Map<String, List<Asset>>> = argumentCaptor()

    private val noFundsObserver: Observer<Event<Unit>> = mock()
    private val noFundsCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    @Test
    fun `refresh balances success`() {
        whenever(walletManager.refreshBalances()).thenReturn(
            Single.just(
                hashMapOf(
                    Pair(
                        "123",
                        Balance(cryptoBalance = BigDecimal.ONE, fiatBalance = BigDecimal.TEN)
                    )
                )
            )
        )
        viewModel.balanceLiveData.observeForever(balanceObserver)
        viewModel.refreshBalances()
        balanceCaptor.run {
            verify(balanceObserver).onChanged(capture())
            firstValue["123"]!!.cryptoBalance == BigDecimal.ONE
        }
    }

    @Test
    fun `refresh balances error`() {
        val error = Throwable()
        whenever(walletManager.refreshBalances()).thenReturn(Single.error(error))
        viewModel.balanceLiveData.observeForever(balanceObserver)
        viewModel.refreshBalances()
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `get assets balance success test`() {
        whenever(walletManager.refreshAssetBalance()).thenReturn(Single.just(mapOf(Pair("test", listOf(Asset("name"))))))
        viewModel.assetBalanceLiveData.observeForever(assetsBalanceObserver)
        viewModel.getAssetBalance()
        assetsBalanceCaptor.run {
            verify(assetsBalanceObserver).onChanged(capture())
            firstValue["test"]!![0].name shouldBe "name"
        }
    }

    @Test
    fun `get assets balance error test`() {
        val error = Throwable()
        whenever(walletManager.refreshAssetBalance()).thenReturn(Single.error(error))
        viewModel.getAssetBalance()
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `Remove value error`() {
        val error = Throwable("error")
        whenever(walletManager.removeValue(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("", ""))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.removeValue(1, "test")
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable("error")
        walletManager.initWalletConfig()
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("", ""))
        whenever(smartContractManager.createSafeAccount(any())).thenReturn(Single.error(error))
        whenever(walletManager.createValue(any(), any(), any(), any())).thenReturn(Completable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.createSafeAccount(Value(index = 1, balance = BigDecimal.ONE))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `create safe account when balance is 0`() {
        walletManager.initWalletConfig()
        whenever(smartContractManager.createSafeAccount(any())).thenReturn(Single.just("address"))
        viewModel.noFundsLiveData.observeForever(noFundsObserver)
        viewModel.createSafeAccount(Value(index = 1, balance = BigDecimal.ZERO))
        noFundsCaptor.run {
            verify(noFundsObserver).onChanged(capture())
        }
    }
}