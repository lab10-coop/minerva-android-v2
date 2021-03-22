package minerva.android.accounts

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.enum.ErrorCode
import minerva.android.accounts.transaction.fragment.AccountsViewModel
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.Balance
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.repository.smartContract.SmartContractRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AccountsViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val smartContractRepository: SmartContractRepository = mock()
    private val accountManager: AccountManager = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val walletConnectRepository: WalletConnectRepository = mock()
    private lateinit var viewModel: AccountsViewModel

    private val balanceObserver: Observer<HashMap<String, Balance>> = mock()
    private val balanceCaptor: KArgumentCaptor<HashMap<String, Balance>> = argumentCaptor()

    private val tokensBalanceObserver: Observer<Map<String, List<AccountToken>>> = mock()
    private val tokensBalanceCaptor: KArgumentCaptor<Map<String, List<AccountToken>>> = argumentCaptor()

    private val noFundsObserver: Observer<Event<Unit>> = mock()
    private val noFundsCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val dappSessionObserver: Observer<HashMap<String, Int>> = mock()
    private val dappSessionCaptor: KArgumentCaptor<HashMap<String, Int>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val refreshBalancesErrorObserver: Observer<Event<ErrorCode>> = mock()
    private val refreshBalancesErrorCaptor: KArgumentCaptor<Event<ErrorCode>> = argumentCaptor()

    private val accountRemoveObserver: Observer<Event<Unit>> = mock()
    private val accountRemoveCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val shouldShowWarningObserver: Observer<Event<Boolean>> = mock()
    private val shouldShowWarningCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()


    @Before
    fun initViewModel() {
        whenever(accountManager.enableMainNetsFlowable).thenReturn(Flowable.just(true))
        viewModel = AccountsViewModel(
            accountManager,
            walletActionsRepository,
            smartContractRepository,
            transactionRepository,
            walletConnectRepository
        )
    }

    @Test
    fun `should show warning success test`() {
        viewModel.shouldShowWarringLiveData.observeForever(shouldShowWarningObserver)
        shouldShowWarningCaptor.run {
            verify(shouldShowWarningObserver).onChanged(capture())
            firstValue.peekContent()
        }
    }

    @Test
    fun `should show warning error test`() {
        val error = Throwable()
        whenever(accountManager.enableMainNetsFlowable).thenReturn(Flowable.error(error))
        viewModel.shouldShowWarringLiveData.observeForever(shouldShowWarningObserver)
        shouldShowWarningCaptor.run {
            verify(shouldShowWarningObserver).onChanged(capture())
            !firstValue.peekContent()
        }
    }

    @Test
    fun `are pending transactions empty`() {
        whenever(transactionRepository.getPendingAccounts()).thenReturn(emptyList())
        val result = viewModel.arePendingAccountsEmpty()
        assertEquals(true, result)
    }

    @Test
    fun `are main nets enabled test`() {
        whenever(accountManager.areMainNetworksEnabled).thenReturn(true)
        val result = viewModel.areMainNetsEnabled
        assertEquals(true, result)
    }

    @Test
    fun `refresh balances success`() {
        whenever(walletConnectRepository.getSessionsFlowable())
            .thenReturn(Flowable.just(listOf(DappSession(address = "address"))))
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
        whenever(accountManager.getAllAccounts()).thenReturn(accounts)
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
        whenever(walletConnectRepository.getSessionsFlowable())
            .thenReturn(Flowable.just(listOf(DappSession(address = "address"))))
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
        whenever(accountManager.getAllAccounts()).thenReturn(accounts)
        whenever(transactionRepository.refreshBalances()).thenReturn(Single.error(error))
        viewModel.refreshBalancesErrorLiveData.observeForever(refreshBalancesErrorObserver)
        viewModel.refreshBalances()
        refreshBalancesErrorCaptor.run {
            verify(refreshBalancesErrorObserver).onChanged(capture())
            firstValue.peekContent() == ErrorCode.BALANCE_ERROR
        }
    }

    @Test
    fun `get tokens balance success test`() {
        whenever(transactionRepository.refreshTokenBalance()).thenReturn(
            Single.just(
                mapOf(
                    Pair(
                        "test",
                        listOf(AccountToken(ERC20Token(1, "name")))
                    )
                )
            )
        )
        viewModel.tokenBalanceLiveData.observeForever(tokensBalanceObserver)
        viewModel.refreshTokenBalance()
        tokensBalanceCaptor.run {
            verify(tokensBalanceObserver).onChanged(capture())
            (firstValue["test"] ?: error(""))[0].token.name shouldBe "name"
        }
    }

    @Test
    fun `get tokens list test`() {
        whenever(transactionRepository.refreshTokensList()).thenReturn(
            Single.just(true),
            Single.just(false),
            Single.error(Throwable("Refresh tokens list error"))
        )
        whenever(transactionRepository.refreshTokenBalance()).thenReturn(Single.just(mapOf()))

        viewModel.refreshTokensList()
        viewModel.refreshTokensList()
        viewModel.refreshTokensList()
        verify(transactionRepository, times(1)).refreshTokenBalance()
    }

    @Test
    fun `get tokens balance error test`() {
        val error = Throwable()
        whenever(transactionRepository.refreshTokenBalance()).thenReturn(Single.error(error))
        viewModel.refreshBalancesErrorLiveData.observeForever(refreshBalancesErrorObserver)
        viewModel.refreshTokenBalance()
        refreshBalancesErrorCaptor.run {
            verify(refreshBalancesErrorObserver).onChanged(capture())
            firstValue.peekContent() == ErrorCode.TOKEN_BALANCE_ERROR
        }
    }

    @Test
    fun `Remove value error`() {
        val error = Throwable("error")
        whenever(accountManager.removeAccount(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(
            Completable.error(error)
        )
        whenever(walletConnectRepository.killAllAccountSessions(any())).thenReturn(Completable.complete())
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
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
        whenever(walletConnectRepository.killAllAccountSessions(any())).thenReturn(Completable.complete())
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
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
        whenever(accountManager.createRegularAccount(any())).thenReturn(Single.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.createSafeAccount(Account(id = 1, cryptoBalance = BigDecimal.ONE))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `create safe account when balance is 0`() {
        whenever(smartContractRepository.createSafeAccount(any())).thenReturn(Single.just("address"))
        viewModel.noFundsLiveData.observeForever(noFundsObserver)
        viewModel.createSafeAccount(Account(id = 1, cryptoBalance = BigDecimal.ZERO))
        noFundsCaptor.run {
            verify(noFundsObserver).onChanged(capture())
        }
    }

    @Test
    fun `get first active artis account`() {
        NetworkManager.initialize(networks)
        val account = viewModel.getAccountForFreeATS(accounts)
        assertEquals(true, account.id == 2)
    }

    @Test
    fun `adding free ATS correct`() {
        NetworkManager.initialize(networks)
        whenever(transactionRepository.getFreeATS(any())).thenReturn(Completable.complete())
        viewModel.addAtsToken(accounts, "nope")
        verify(accountManager, times(1)).saveFreeATSTimestamp()
    }

    @Test
    fun `adding free ATS error`() {
        NetworkManager.initialize(networks)
        viewModel.errorLiveData.observeForever(errorObserver)
        whenever(transactionRepository.getFreeATS(any())).thenReturn(Completable.error(Throwable("Some error")))
        viewModel.addAtsToken(accounts, "nope")
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `missing account for adding free ATS test error`() {
        viewModel.errorLiveData.observeForever(errorObserver)
        whenever(accountManager.getLastFreeATSTimestamp()).thenReturn(0)
        viewModel.addAtsToken(listOf(), "nope")
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `check that last free ATS was at least 24 hours (86400000 mills) ago`() {
        NetworkManager.initialize(networks)
        whenever(accountManager.currentTimeMills()).thenReturn(1610120569428)
        accountManager.currentTimeMills().let { time ->
            whenever(accountManager.getLastFreeATSTimestamp()).thenReturn(
                time - 96400000,
                time - 86400001,
                time - 86299933,
                time - 500,
                time - 96400000,
                time - 303
            )
        }
        assertEquals(true, viewModel.isAddingFreeATSAvailable(accounts))
        assertEquals(true, viewModel.isAddingFreeATSAvailable(accounts))
        assertEquals(false, viewModel.isAddingFreeATSAvailable(accounts))
        assertEquals(false, viewModel.isAddingFreeATSAvailable(accounts))
        assertEquals(false, viewModel.isAddingFreeATSAvailable(accountsWithoutPrimaryAccount))
        assertEquals(false, viewModel.isAddingFreeATSAvailable(accountsWithoutPrimaryAccount))
    }

    @Test
    fun `get sessions and update accounts success`() {
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(
            Flowable.just(
                listOf(DappSession(address = "address"))
            )
        )
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
        viewModel.dappSessions.observeForever(dappSessionObserver)
        viewModel.getSessions(accounts)
        dappSessionCaptor.run {
            verify(dappSessionObserver).onChanged(capture())
            firstValue["address"] == 1
        }
    }

    @Test
    fun `get sessions and update accounts error`() {
        val error = Throwable()
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.error(error))
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.getSessions(accounts)
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `no sessions so account list is not updated, so test should fail`() {
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.just(emptyList()))
        viewModel.dappSessions.observeForever(dappSessionObserver)
        viewModel.getSessions(accounts)
        dappSessionCaptor.run {
            assertFails { firstValue }
        }
    }

    @Test
    fun `Check if calling getTokenVisibility() method is calling the method`() {
        viewModel.tokenVisibilitySettings = mock()
        viewModel.tokenVisibilitySettings.let { settings ->
            val erc20Token = ERC20Token(1, address = "0xC00KiE", decimals = "2")
            whenever(settings.getTokenVisibility(any(), any())).thenReturn(false, false, true, true, null)
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ONE)) shouldBeEqualTo false
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ZERO)) shouldBeEqualTo false
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ONE)) shouldBeEqualTo true
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ZERO)) shouldBeEqualTo false
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ONE)) shouldBeEqualTo null
            verify(settings, times(5)).getTokenVisibility(any(), any())
        }
    }

    private val accounts = listOf(
        Account(1, chainId = 2),
        Account(2, chainId = 1),
        Account(3, chainId = 1),
        Account(4, chainId = 3)
    )

    private val accountsWithoutPrimaryAccount = listOf(
        Account(1, chainId = 2),
        Account(4, chainId = 3)
    )

    private val networks = listOf(
        Network(httpRpc = "some_rpc", chainId = 1),
        Network(httpRpc = "some_rpc", chainId = 2),
        Network(httpRpc = "some_rpc", chainId = 3)
    )
}