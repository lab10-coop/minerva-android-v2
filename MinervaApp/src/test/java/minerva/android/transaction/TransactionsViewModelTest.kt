package minerva.android.transaction

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.values.transaction.TransactionsViewModel
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.smartContract.SmartContractRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class TransactionViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val smartContractRepository: SmartContractRepository = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val viewModel = TransactionsViewModel(walletActionsRepository, smartContractRepository, transactionRepository)

    private val sendTransactionObserver: Observer<Event<Pair<String, Int>>> = mock()
    private val sendTransactionCaptor: KArgumentCaptor<Event<Pair<String, Int>>> = argumentCaptor()

    private val saveActionFailedCaptor: KArgumentCaptor<Event<Pair<String, Int>>> = argumentCaptor()

    @Test
    fun `send main transaction test success and wallet action succeed`() {
        whenever(transactionRepository.transferNativeCoin(any(), any())).thenReturn(Single.just("hash"))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        sendTransactionCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send main transaction test success and wallet action failed`() {
        val error = Throwable()
        whenever(transactionRepository.transferNativeCoin(any(), any())).thenReturn(Single.just("hash"))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("name"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.saveWalletActionFailedLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        saveActionFailedCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send main transaction test error and send wallet action succeed`() {
        val error = Throwable()
        whenever(transactionRepository.transferNativeCoin(any(), any())).thenReturn(Single.error(error))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        sendTransactionCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send main transaction test error and send wallet action failed`() {
        val error = Throwable()
        whenever(transactionRepository.transferNativeCoin(any(), any())).thenReturn(Single.error(error))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `send safe account main transaction test success`() {
        viewModel.value = Value(
            index = 0,
            owners = listOf("tom", "beata", "bogdan"),
            publicKey = "12",
            privateKey = "12",
            address = "address",
            contractAddress = "aa"
        )
        whenever(transactionRepository.transferNativeCoin(any(), any())).thenReturn(Single.just("hash"))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(smartContractRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        sendTransactionCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send safe account main transaction test error`() {
        val error = Throwable()
        viewModel.value = Value(
            index = 0,
            owners = listOf("tom", "beata", "bogdan"),
            publicKey = "12",
            privateKey = "12",
            address = "address",
            contractAddress = "aa",
            bindedOwner = "binded"
        )
        whenever(transactionRepository.transferNativeCoin(any(), any())).thenReturn(Single.just("hash"))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(smartContractRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `send asset main transaction test success`() {
        viewModel.apply {
            value = Value(
                index = 0,
                publicKey = "12",
                privateKey = "12",
                address = "address",
                contractAddress = "aa",
                assets = listOf(Asset("name"))
            )
            assetIndex = 0
        }
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        sendTransactionCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send asset main transaction test error`() {
        val error = Throwable()
        viewModel.apply {
            value = Value(
                index = 0, publicKey = "12", privateKey = "12", address = "address", contractAddress = "aa",
                assets = listOf(Asset("name"))
            )
            assetIndex = 0
        }
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.error(error))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        viewModel.run {
            sendTransactionLiveData.observeForever(sendTransactionObserver)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    @Test
    fun `send safe account asset transaction test success`() {
        viewModel.value = Value(
            index = 0,
            assets = listOf(Asset("name")),
            publicKey = "12",
            privateKey = "12",
            address = "address",
            contractAddress = "aa"
        )
        viewModel.assetIndex = 0
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(smartContractRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        sendTransactionCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send safe account asset transaction test error`() {
        val error = Throwable()
        viewModel.value = Value(
            index = 0,
            assets = listOf(Asset("name")),
            publicKey = "12",
            privateKey = "12",
            address = "address",
            contractAddress = "aa"
        )
        viewModel.assetIndex = 0
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.error(error))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(smartContractRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `calculate transaction cost test`() {
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal(4))
        val result = viewModel.calculateTransactionCost(BigDecimal(2), BigInteger.valueOf(2))
        result shouldBeEqualTo BigDecimal(4).toPlainString()
    }

    @Test
    fun `get available funds test`() {
        viewModel.value.cryptoBalance = BigDecimal.valueOf(5)
        viewModel.transactionCost = BigDecimal.valueOf(1)
        val result = viewModel.getAllAvailableFunds()
        result shouldBeEqualTo "4"
    }

    @Test
    fun `prepare prefix address test`() {
        val result = viewModel.preparePrefixAddress("prefixAddress", "prefix")
        assertEquals(result, "Address")
    }

    @Test
    fun`is correct network test`(){
        viewModel.value = Value(0, name = "prefixName")
        val result = viewModel.isCorrectNetwork("prefix")
        assertEquals(result, true)
    }
}