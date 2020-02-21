package minerva.android.transaction

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.values.transaction.TransactionsViewModel
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.manager.walletActions.WalletActionsRepository
import minerva.android.walletmanager.model.MasterKey
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.TransactionCost
import minerva.android.walletmanager.model.Value
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class TransactionViewModelTest : BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val walletActionsRepository: WalletActionsRepository = mock()
    private val viewModel = TransactionsViewModel(walletManager, walletActionsRepository)

    private val transactionCostObserver: Observer<Event<TransactionCost>> = mock()
    private val transactionCostCaptor: KArgumentCaptor<Event<TransactionCost>> = argumentCaptor()

    private val sendTransactionObserver: Observer<Event<Unit>> = mock()
    private val sendTransactionCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val saveWalletActionObserver: Observer<Event<Pair<String, Int>>> = mock()
    private val saveWalletActionCaptor: KArgumentCaptor<Event<Pair<String, Int>>> = argumentCaptor()

    @Test
    fun `get transaction cost test success`() {
        whenever(walletManager.getTransactionCosts(any(), any())).thenReturn(
            Single.just(
                TransactionCost(
                    BigDecimal(1),
                    BigInteger.ONE,
                    BigDecimal(10)
                )
            )
        )
        whenever(walletManager.getTransactionCosts(any(), any())).thenReturn(Single.just(TransactionCost(BigDecimal(1), BigInteger.ONE, BigDecimal(10))))
        viewModel.transactionCostLiveData.observeForever(transactionCostObserver)
        viewModel.getTransactionCosts()
        transactionCostCaptor.run {
            verify(transactionCostObserver).onChanged(capture())
            firstValue.peekContent().cost == BigDecimal(10)
        }
    }

    @Test
    fun `get transaction cost test error`() {
        val error = Throwable()
        whenever(walletManager.getTransactionCosts(any(), any())).thenReturn(Single.error(error))
        viewModel.transactionCostLiveData.observeForever(transactionCostObserver)
        viewModel.getTransactionCosts()
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `send transaction test success`() {
        whenever(walletManager.sendTransaction(any(), any())).thenReturn(Completable.complete())
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        sendTransactionCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send transaction test error`() {
        val error = Throwable()
        whenever(walletManager.sendTransaction(any(), any())).thenReturn(Completable.error(error))
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        viewModel.errorTransactionLiveData.observeLiveDataEvent(Event(error.message))
    }

    @Test
    fun `save wallet action success`() {
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.complete())
        whenever(walletManager.masterKey).thenReturn(MasterKey("12", "34"))
        viewModel.apply {
            transaction = Transaction(amount = BigDecimal.ONE)
            value = Value(1, network = "eth")
            saveWalletActionLiveData.observeForever(saveWalletActionObserver)
            saveWalletAction(1)
            saveWalletActionCaptor.run {
                verify(saveWalletActionObserver).onChanged(capture())
                firstValue.peekContent().first == "eth"
            }
        }
    }

    @Test
    fun `save wallet action error`() {
        val error = Throwable()
        whenever(walletActionsRepository.saveWalletActions(any(), any())).thenReturn(Completable.error(error))
        whenever(walletManager.masterKey).thenReturn(MasterKey("12", "34"))
        viewModel.apply {
            transaction = Transaction(amount = BigDecimal.ONE)
            value = Value(1, network = "eth")
            saveWalletActionLiveData.observeForever(saveWalletActionObserver)
            saveWalletAction(1)
            errorTransactionLiveData.observeLiveDataEvent(Event(error.message))
        }
    }

    @Test
    fun `calculate transaction cost test`() {
        whenever(walletManager.calculateTransactionCost(any(), any())).thenReturn(BigDecimal(4))
        val result = viewModel.calculateTransactionCost(BigDecimal(2), BigInteger.valueOf(2))
        result shouldBeEqualTo BigDecimal(4).toPlainString()
    }

    @Test
    fun `get available funds test`() {
        viewModel.value.balance = BigDecimal.valueOf(5)
        viewModel.transactionCost = BigDecimal.valueOf(1)
        val result = viewModel.getAllAvailableFunds()
        result shouldBeEqualTo "4"
    }
}