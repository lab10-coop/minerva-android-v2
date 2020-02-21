package minerva.android.values

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.Balance
import org.junit.Test
import java.math.BigDecimal

class ValuesViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = ValuesViewModel(walletManager, walletActionsRepository)

    private val balanceObserver: Observer<HashMap<String, Balance>> = mock()
    private val balanceCaptor: KArgumentCaptor<HashMap<String, Balance>> = argumentCaptor()

    @Test
    fun `refresh balances success`() {
        whenever(walletManager.refreshBalances()).thenReturn(Single.just(hashMapOf(Pair("123", Balance(cryptoBalance = BigDecimal.ONE, fiatBalance = BigDecimal.TEN)))))
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
    fun `Remove value success`() {
        whenever(walletManager.removeValue(any())).thenReturn(Completable.complete())
        val test = walletManager.removeValue(any()).test()
        test.assertNoErrors()
    }

    @Test
    fun `Remove value error`() {
        val error = Throwable("error")
        whenever(walletManager.removeValue(any())).thenReturn(Completable.error(error))
        val test = walletManager.removeValue(any()).test()
        test.assertError(error)
    }
}