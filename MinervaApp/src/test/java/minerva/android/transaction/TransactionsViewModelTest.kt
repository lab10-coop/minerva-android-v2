package minerva.android.transaction

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.transaction.fragment.TransactionViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.TransactionCost
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class TransactionViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val safeAccountRepository: SafeAccountRepository = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val viewModel = TransactionViewModel(walletActionsRepository, safeAccountRepository, transactionRepository)

    private val sendTransactionObserver: Observer<Event<Pair<String, Int>>> = mock()
    private val sendTransactionCaptor: KArgumentCaptor<Event<Pair<String, Int>>> = argumentCaptor()

    private val transactionCompletedObserver: Observer<Event<Any>> = mock()
    private val transactionCompletedCaptor: KArgumentCaptor<Event<Any>> = argumentCaptor()

    private val getGasLimitObserver: Observer<Event<TransactionCost>> = mock()
    private val getGasLimitCaptor: KArgumentCaptor<Event<TransactionCost>> = argumentCaptor()

    private val saveActionFailedCaptor: KArgumentCaptor<Event<Pair<String, Int>>> = argumentCaptor()


    private val networks = listOf(
        Network(chainId = 1, httpRpc = "address", testNet = true),
        Network(chainId = 2, httpRpc = "address", testNet = true),
        Network(chainId = 3, httpRpc = "address", testNet = true, token = "cookie")
    )

    @Before
    fun init() {
        NetworkManager.initialize(networks)
        whenever(transactionRepository.getCoinFiatRate(any())).doReturn(Single.just(1.5))
    }

    @Test
    fun `getting token list`() {
        viewModel.account = Account(
            id = 0,
            publicKey = "12",
            privateKey = "12",
            chainId = 3,
            address = "address",
            contractAddress = "aa",
            bindedOwner = "binded",
            accountTokens = mutableListOf(AccountToken(ERC20Token(3, symbol = "SomeSymbol"), BigDecimal.ZERO))
        )

        val tokenList = viewModel.tokensList
        tokenList.size shouldBeEqualTo 2
        tokenList[0].token.symbol shouldBeEqualTo "cookie"
        tokenList[1].token.symbol shouldBeEqualTo "SomeSymbol"
    }

    @Test
    fun `send main transaction test success and wallet action succeed`() {
        whenever(transactionRepository.transferNativeCoin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just(""))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, chainId = 1))
        NetworkManager.initialize(listOf(Network(chainId = 1, httpRpc = "some")))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            getAccount(0, String.Empty)
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
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, chainId = 3))
        viewModel.run {
            saveWalletActionFailedLiveData.observeForever(sendTransactionObserver)
            getAccount(0, String.Empty)
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
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, chainId = 3))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            getAccount(0, String.Empty)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        }
        transactionCompletedCaptor.run {
            verify(transactionCompletedObserver).onChanged(capture())
        }
    }

    @Test
    fun `send safe account main transaction test success`() {
        whenever(transactionRepository.transferNativeCoin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(safeAccountRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, chainId = 3))
        viewModel.run {
            transactionCompletedLiveData.observeForever(transactionCompletedObserver)
            getAccount(0, String.Empty)
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
            id = 0,
            owners = listOf("tom", "beata", "bogdan"),
            publicKey = "12",
            privateKey = "12",
            chainId = 3,
            address = "address",
            contractAddress = "aa",
            bindedOwner = "binded"
        )
        whenever(transactionRepository.transferNativeCoin(any(), any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(safeAccountRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"

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
                id = 0,
                publicKey = "12",
                privateKey = "12",
                address = "address",
                chainId = 1,
                contractAddress = "aa",
                accountTokens = mutableListOf(AccountToken(ERC20Token(3, "name", decimals = "3", address = "0x0")))
            )
            tokenAddress = "0x0"
        }
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
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
                id = 0, publicKey = "12", privateKey = "12", address = "address", contractAddress = "aa",
                chainId = 3, accountTokens = mutableListOf(AccountToken(ERC20Token(3, "name", address = "0x0", decimals = "3")))
            )
            tokenAddress = "0x0"
        }
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.error(error))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        viewModel.run {
            sendTransactionLiveData.observeForever(sendTransactionObserver)
            sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    @Test
    fun `send safe account asset transaction test success`() {
        viewModel.account = Account(
            id = 0,
            accountTokens = mutableListOf(AccountToken(ERC20Token(3, "name", decimals = "3", address = "0x0"))),
            publicKey = "12",
            privateKey = "12",
            address = "address",
            chainId = 3,
            contractAddress = "aa"
        )
        viewModel.tokenAddress = "0x0"
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(safeAccountRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
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
            id = 0,
            accountTokens = mutableListOf(AccountToken(ERC20Token(3, "name", decimals = "3", address = "0x0"))),
            publicKey = "12",
            privateKey = "12",
            chainId = 3,
            address = "address",
            contractAddress = "aa"
        )
        viewModel.tokenAddress = "0x0"
        whenever(transactionRepository.transferERC20Token(any(), any())).thenReturn(Completable.error(error))
        whenever(transactionRepository.resolveENS(any())).thenReturn(Single.just("tom"))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(safeAccountRepository.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"

        viewModel.sendTransactionLiveData.observeForever(sendTransactionObserver)
        viewModel.sendTransaction("123", BigDecimal(12), BigDecimal(1), BigInteger.ONE)
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
    }

    @Test
    fun `calculate transaction cost test`() {
        whenever(transactionRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal(4))
        val result = viewModel.calculateTransactionCost(BigDecimal(2), BigInteger.valueOf(2))
        result shouldBeEqualTo BigDecimal(4)
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
        val account = Account(
            id = 0,
            publicKey = "12",
            chainId = 1,
            accountTokens = mutableListOf(
                AccountToken(
                    ERC20Token(3, symbol = "SomeSymbol", decimals = "2", address = "0x0"),
                    BigDecimal.ZERO
                )
            )
        )
        whenever(transactionRepository.getTransactionCosts(any())).doReturn(
            Single.just(
                TransactionCost(BigDecimal.TEN, BigInteger.ONE, BigDecimal.TEN)
            )
        )
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, chainId = 3))
        whenever(transactionRepository.getAccount(any())).thenReturn(account)
        viewModel.run {
            transactionCostLiveData.observeForever(getGasLimitObserver)
            getAccount(1, "0x0")
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
        whenever(transactionRepository.getTransactionCosts(any())).doReturn(Single.error(error))
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, chainId = 1))
        viewModel.run {
            getAccount(0, String.Empty)
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

    @Test
    fun `is update fiat rate and recalculate fiat amount valid for coins`() {
        whenever(transactionRepository.getAccount(any())).thenReturn(Account(0, chainId = 1, coinRate = 2.0))
        NetworkManager.initialize(listOf(Network(chainId = 1, httpRpc = "some")))
        viewModel.run {
            getAccount(0, String.Empty)
            updateFiatRate()
            recalculateFiatAmount(BigDecimal("10.51")) shouldBeEqualTo BigDecimal("21.02")
        }
    }

    @Test
    fun `is update fiat rate and recalculate fiat amount valid for tokens`() {
        whenever(transactionRepository.getAccount(any())).thenReturn(
            Account(
                0,
                chainId = 1,
                accountTokens = mutableListOf(AccountToken(ERC20Token(1, address = "address01"), BigDecimal.TEN, 5.0))
            )
        )
        NetworkManager.initialize(listOf(Network(chainId = 1, httpRpc = "some")))
        viewModel.run {
            getAccount(0, String.Empty)
            updateTokenAddress(0)
            updateFiatRate()
            recalculateFiatAmount(BigDecimal("1.01")) shouldBeEqualTo BigDecimal("5.05")
        }
    }
}