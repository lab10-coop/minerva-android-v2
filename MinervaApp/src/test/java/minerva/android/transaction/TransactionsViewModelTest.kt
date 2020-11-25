package minerva.android.transaction

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.transaction.TransactionsViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.*
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

    private val transactionCompletedObserver: Observer<Event<Any>> = mock()
    private val transactionCompletedCaptor: KArgumentCaptor<Event<Any>> = argumentCaptor()

    private val getGasLimitObserver: Observer<Event<TransactionCost>> = mock()
    private val getGasLimitCaptor: KArgumentCaptor<Event<TransactionCost>> = argumentCaptor()

    private val saveActionFailedCaptor: KArgumentCaptor<Event<Pair<String, Int>>> = argumentCaptor()

    @Test
    fun `send main transaction test success and wallet action succeed`() {
        whenever(transactionRepository.transferNativeCoin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, network = Network(short = "aaa")))
        NetworkManager.initialize(listOf(Network(short = "aaa", httpRpc = "some")))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            getAccount(0, -1)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
        }
    }

    @Test
    fun `send main transaction test success and wallet action failed`() {
        val error = Throwable()
        whenever(transactionRepository.transferNativeCoin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("name"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, network = Network(short = "aaa")))
        NetworkManager.initialize(listOf(Network(short = "aaa", httpRpc = "some")))
        viewModel.run {
            saveWalletActionFailedLiveData.observeForever(sendTransactionObserver)
            getAccount(0, -1)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        }
        saveActionFailedCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send main transaction test error and send wallet action succeed`() {
        whenever(transactionRepository.transferNativeCoin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
        }
    }

    @Test
    fun `send safe account main transaction test success`() {
        viewModel.account = Account(
            index = 0,
            owners = listOf("tom", "beata", "bogdan"),
            publicKey = "12",
            privateKey = "12",
            address = "address",
            contractAddress = "aa"
        )
        whenever(transactionRepository.transferNativeCoin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(smartContractRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, network = Network(short = "aaa")))
        NetworkManager.initialize(listOf(Network(short = "aaa", httpRpc = "some")))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            getAccount(0, -1)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
        }
    }

    @Test
    fun `send safe account main transaction test error`() {
        val error = Throwable()
        viewModel.account = Account(
            index = 0,
            owners = listOf("tom", "beata", "bogdan"),
            publicKey = "12",
            privateKey = "12",
            address = "address",
            contractAddress = "aa",
            bindedOwner = "binded"
        )
        whenever(transactionRepository.transferNativeCoin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(smartContractRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
        viewModel.run {
            sendTransactionLiveData.observeForever(sendTransactionObserver)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    @Test
    fun `send asset main transaction test success`() {
        viewModel.apply {
            account = Account(
                index = 0,
                publicKey = "12",
                privateKey = "12",
                address = "address",
                contractAddress = "aa",
                accountAssets = listOf(AccountAsset(Asset("name")))
            )
            assetIndex = 0
        }
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        viewModel.run {
            sendTransactionLiveData.observeForever(sendTransactionObserver)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        }
        sendTransactionCaptor.run {
            verify(sendTransactionObserver).onChanged(capture())
        }
    }

    @Test
    fun `send asset main transaction test error`() {
        val error = Throwable()
        viewModel.apply {
            account = Account(
                index = 0, publicKey = "12", privateKey = "12", address = "address", contractAddress = "aa",
                accountAssets = listOf(AccountAsset(Asset("name")))
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
        viewModel.account = Account(
            index = 0,
            accountAssets = listOf(AccountAsset(Asset("name"))),
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
        viewModel.account = Account(
            index = 0,
            accountAssets = listOf(AccountAsset(Asset("name"))),
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
        viewModel.account.cryptoBalance = BigDecimal.valueOf(5)
        viewModel.transactionCost = BigDecimal.valueOf(1)
        val result = viewModel.getAllAvailableFunds()
        result shouldBeEqualTo "4"
    }

    @Test
    fun `fetch gas limit success`() {
        whenever(transactionRepository.getTransactionCosts(any(), any(), any(), any(), any())).doReturn(Single.just(TransactionCost(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN)))
        viewModel.run {
            transactionCostLiveData.observeForever(getGasLimitObserver)
            getTransactionCosts("address", BigDecimal.TEN)
        }
        getGasLimitCaptor.run {
            verify(getGasLimitObserver).onChanged(capture())
            firstValue.peekContent().gasPrice == BigDecimal.TEN
        }
    }

    @Test
    fun `fetch gas limit error`() {
        val error = Throwable()
        whenever(transactionRepository.getTransactionCosts(any(), any(), any(), any(), any())).doReturn(Single.error(error))
        viewModel.run {
            getTransactionCosts("address", BigDecimal.TEN)
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    @Test
    fun `is address valid success`() {
        whenever(transactionRepository.isAddressValid(any())).thenReturn(true)
        val result = viewModel.isAddressValid("0x12345")
        assertEquals(true, result)
    }

    @Test
    fun `is address valid false`() {
        whenever(transactionRepository.isAddressValid(any())).thenReturn(false)
        val result = viewModel.isAddressValid("eeee")
        assertEquals(false, result)
    }
}