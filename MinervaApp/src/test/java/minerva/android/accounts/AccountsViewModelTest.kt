package minerva.android.accounts

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Balance
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.smartContract.SmartContractRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBe
import org.junit.Test
import java.math.BigDecimal

class AccountsViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val smartContractRepository: SmartContractRepository = mock()
    private val accountManager: AccountManager = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val viewModel = AccountsViewModel(accountManager, walletActionsRepository, smartContractRepository, transactionRepository)

    private val balanceObserver: Observer<HashMap<String, Balance>> = mock()
    private val balanceCaptor: KArgumentCaptor<HashMap<String, Balance>> = argumentCaptor()

    private val assetsBalanceObserver: Observer<Map<String, List<Asset>>> = mock()
    private val assetsBalanceCaptor: KArgumentCaptor<Map<String, List<Asset>>> = argumentCaptor()

    private val noFundsObserver: Observer<Event<Unit>> = mock()
    private val noFundsCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val accountRemoveObserver: Observer<Event<Unit>> = mock()
    private val accountRemoveCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Test
    fun `refresh balances success`() {
        whenever(transactionRepository.refreshBalances()).thenReturn(
            Single.just(hashMapOf(Pair("123", Balance(cryptoBalance = BigDecimal.ONE, fiatBalance = BigDecimal.TEN))))
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
        whenever(transactionRepository.refreshBalances()).thenReturn(Single.error(error))
        viewModel.balanceLiveData.observeForever(balanceObserver)
        viewModel.refreshBalances()
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `get assets balance success test`() {
        whenever(transactionRepository.refreshAssetBalance()).thenReturn(Single.just(mapOf(Pair("test", listOf(Asset("name"))))))
        viewModel.assetBalanceLiveData.observeForever(assetsBalanceObserver)
        viewModel.getAssetBalance()
        assetsBalanceCaptor.run {
            verify(assetsBalanceObserver).onChanged(capture())
            (firstValue["test"] ?: error(""))[0].name shouldBe "name"
        }
    }

    @Test
    fun `get assets balance error test`() {
        val error = Throwable()
        whenever(transactionRepository.refreshAssetBalance()).thenReturn(Single.error(error))
        viewModel.getAssetBalance()
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `Remove value error`() {
        val error = Throwable("error")
        whenever(accountManager.removeAccount(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.removeAccount(Account(1, "test"))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `Remove value success`() {
        whenever(accountManager.removeAccount(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.accountRemovedLiveData.observeForever(accountRemoveObserver)
        viewModel.removeAccount(Account(1, "test"))
        accountRemoveCaptor.run {
            verify(accountRemoveObserver).onChanged(capture())
        }
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable("error")
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(smartContractRepository.createSafeAccount(any())).thenReturn(Single.error(error))
        whenever(accountManager.createAccount(any(), any(), any(), any())).thenReturn(Completable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.createSafeAccount(Account(index = 1, cryptoBalance = BigDecimal.ONE))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `create safe account when balance is 0`() {
        whenever(smartContractRepository.createSafeAccount(any())).thenReturn(Single.just("address"))
        viewModel.noFundsLiveData.observeForever(noFundsObserver)
        viewModel.createSafeAccount(Account(index = 1, cryptoBalance = BigDecimal.ZERO))
        noFundsCaptor.run {
            verify(noFundsObserver).onChanged(capture())
        }
    }
}