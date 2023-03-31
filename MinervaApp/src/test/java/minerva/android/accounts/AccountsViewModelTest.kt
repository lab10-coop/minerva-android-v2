package minerva.android.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.state.*
import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.kotlinUtils.event.Event
import minerva.android.mock.accounts
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.accounts.tokens.TokenManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.AssetBalance
import minerva.android.walletmanager.model.minervaprimitives.account.CoinBalance
import minerva.android.walletmanager.model.minervaprimitives.account.CoinError
import minerva.android.walletmanager.model.network.Network
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERCToken
import minerva.android.walletmanager.model.token.TokenType
import minerva.android.walletmanager.model.transactions.Balance
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.model.walletconnect.DappSessionV1
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AccountsViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val safeAccountRepository: SafeAccountRepository = mock()
    private val accountManager: AccountManager = mock()
    private val tokenManager: TokenManager = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val walletConnectRepository: WalletConnectRepository = mock()
    private val logger: Logger = mock()
    private lateinit var viewModel: AccountsViewModel

    private val balanceObserver: Observer<BalanceState> = mock()
    private val balanceCaptor: KArgumentCaptor<BalanceState> = argumentCaptor()

    private val dappSessionObserver: Observer<List<DappSessionData>> = mock()
    private val dappSessionCaptor: KArgumentCaptor<List<DappSessionData>> = argumentCaptor()

    private val errorObserver: Observer<Event<AccountsErrorState>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<AccountsErrorState>> = argumentCaptor()

    private val accountRemoveObserver: Observer<Event<Unit>> = mock()
    private val accountRemoveCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val loadingObserver: Observer<Event<Boolean>> = mock()
    private val loadingCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    private val ratesMapLiveData: LiveData<Event<Unit>> = mock()
    private val walletConfigLiveData: LiveData<Event<WalletConfig>> = mock()
    private val balancesInsertLiveData: LiveData<Event<Unit>> = mock()

    @Before
    fun initViewModel() {
        whenever(transactionRepository.ratesMapLiveData).thenReturn(ratesMapLiveData)
        whenever(accountManager.walletConfigLiveData).thenReturn(walletConfigLiveData)
        whenever(accountManager.balancesInsertLiveData).thenReturn(balancesInsertLiveData)

        viewModel = AccountsViewModel(
            accountManager,
            tokenManager,
            walletActionsRepository,
            safeAccountRepository,
            transactionRepository,
            walletConnectRepository,
            logger
        )
    }

    @Test
    fun `are pending transactions empty`() {
        whenever(transactionRepository.getPendingAccounts()).thenReturn(emptyList())
        val result = viewModel.arePendingAccountsEmpty
        assertEquals(false, result)
    }

    @Test
    fun `are main nets enabled test`() {
        whenever(accountManager.areMainNetworksEnabled).thenReturn(true)
        val result = viewModel.areMainNetsEnabled
        assertEquals(true, result)
    }

    @Test
    fun `refresh balances coin balance test`() {
        whenever(walletConnectRepository.getSessionsFlowable())
            .thenReturn(Flowable.just(listOf(DappSessionV1(address = "address"))))
        whenever(accountManager.toChecksumAddress(any(), anyOrNull())).thenReturn("address")
        whenever(accountManager.getAllAccounts()).thenReturn(accounts)
        whenever(transactionRepository.getCoinBalance())
            .thenReturn(
                Flowable.just(
                    CoinBalance(
                        1, "123",
                        Balance(cryptoBalance = BigDecimal.ONE, fiatBalance = BigDecimal.TEN),
                        1.0
                    )
                )
            )
        viewModel.balanceStateLiveData.observeForever(balanceObserver)
        viewModel.refreshCoinBalances()
        balanceCaptor.run {
            verify(balanceObserver).onChanged(capture())
            firstValue as CoinBalanceCompleted
        }
    }

    @Test
    fun `refresh balances return coin balance error test`() {
        val error = Throwable("Error Balance")
        whenever(walletConnectRepository.getSessionsFlowable())
            .thenReturn(Flowable.just(listOf(DappSessionV1(address = "address"))))
        whenever(accountManager.toChecksumAddress(any(), anyOrNull())).thenReturn("address")
        whenever(accountManager.getAllAccounts()).thenReturn(accounts)
        whenever(transactionRepository.getCoinBalance())
            .thenReturn(Flowable.just(CoinError(1, "123", error)))
        viewModel.balanceStateLiveData.observeForever(balanceObserver)
        viewModel.refreshCoinBalances()
        balanceCaptor.run {
            verify(balanceObserver).onChanged(capture())
            firstValue as CoinBalanceCompleted
        }
    }


    @Test
    fun `refresh balances return error test`() {
        val error = Throwable("Error Balance")
        whenever(walletConnectRepository.getSessionsFlowable())
            .thenReturn(Flowable.just(listOf(DappSessionV1(address = "address"))))
        whenever(accountManager.toChecksumAddress(any(), anyOrNull())).thenReturn("address")
        whenever(accountManager.getAllAccounts()).thenReturn(accounts)
        whenever(transactionRepository.getCoinBalance())
            .thenReturn(Flowable.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.refreshCoinBalances()
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
            firstValue.peekContent() as RefreshBalanceError
        }
    }

    @Test
    fun `get tokens balance success when tagged tokens are not empty test`() {
        whenever(transactionRepository.getTokensUpdate())
            .thenReturn(Flowable.just(listOf(ERCToken(1, "token", type = TokenType.ERC20))))
        whenever(transactionRepository.getTokenBalance())
            .thenReturn(Flowable.just(AssetBalance(1, "test", AccountToken(ERCToken(1, "name", type = TokenType.ERC20)))))
        whenever(transactionRepository.updateTokens()).thenReturn(Completable.complete())
        viewModel.refreshTokensBalances()
        viewModel.balanceStateLiveData.observeForever(balanceObserver)
        balanceCaptor.run {
            verify(balanceObserver).onChanged(capture())
        }
    }

    @Test
    fun `get tokens balance success when tagged tokens are empty test`() {
        whenever(transactionRepository.getTokensUpdate()).thenReturn(Flowable.just(emptyList()))
        whenever(transactionRepository.getTokenBalance())
            .thenReturn(Flowable.just(AssetBalance(1, "test", AccountToken(ERCToken(1, "name", type = TokenType.ERC20)))))
        viewModel.refreshTokensBalances()
        viewModel.balanceStateLiveData.observeForever(balanceObserver)
        balanceCaptor.run {
            verifyNoMoreInteractions(balanceObserver)
        }
    }

    @Test
    fun `get tokens balance success when tagged tokens are not empty and check tokens visibility test`() {
        whenever(transactionRepository.getTokensUpdate())
            .thenReturn(
                Flowable.just(listOf(ERCToken(1, "name1", address = "tokenAddress1", tag = "tag", type = TokenType.ERC20)))
            )
        whenever(transactionRepository.getTokenBalance())
            .thenReturn(Flowable.just(AssetBalance(1, "test", AccountToken(ERCToken(1, "name", type = TokenType.ERC20)))))
        whenever(transactionRepository.updateTokens()).thenReturn(Completable.complete())

        whenever(accountManager.activeAccounts).thenReturn(
            listOf(
                Account(
                    1,
                    privateKey = "privateKey1",
                    address = "address1"
                ),
                Account(
                    2,
                    privateKey = "privateKey2",
                    address = "address2"
                )
            )
        )
        viewModel.tokenVisibilitySettings = mock()
        with(viewModel.tokenVisibilitySettings) {
            whenever(getTokenVisibility("address1", "tokenAddress1")).thenReturn(true)
            whenever(getTokenVisibility("address1", "tokenAddress2")).thenReturn(false)
            whenever(getTokenVisibility("address2", "tokenAddress3")).thenReturn(true)
            whenever(getTokenVisibility("address2", "tokenAddress4")).thenReturn(true)
        }
        viewModel.refreshTokensBalances()
        viewModel.balanceStateLiveData.observeForever(balanceObserver)
        balanceCaptor.run {
            verify(balanceObserver).onChanged(capture())
        }
    }

    @Test
    fun `get tokens balance error test`() {
        val error = Throwable()
        whenever(transactionRepository.getTokenBalance()).thenReturn(Flowable.error(error))
        whenever(transactionRepository.getTokensUpdate()).thenReturn(
            Flowable.just(
                listOf(
                    ERCToken(
                        1,
                        "token",
                        type = TokenType.ERC20
                    )
                )
            )
        )
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.refreshTokensBalances()
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
            firstValue.peekContent() == RefreshBalanceError
        }
    }

    @Test
    fun `get tokens list test`() {
        whenever(transactionRepository.discoverNewTokens()).thenReturn(
            Single.just(true),
            Single.just(false),
            Single.error(Throwable("Refresh tokens list error"))
        )
        whenever(transactionRepository.getTokensUpdate())
            .thenReturn(Flowable.just(listOf(ERCToken(1, "token", type = TokenType.ERC20))))
        whenever(walletConnectRepository.getSessionsFlowable())
            .thenReturn(Flowable.just(listOf(DappSessionV1(address = "address"))))
        whenever(transactionRepository.getTokenBalance())
            .thenReturn(Flowable.just(AssetBalance(1, "test", AccountToken(ERCToken(1, "name", type = TokenType.ERC20)))))
        whenever( tokenManager.fetchNFTsDetails()).thenReturn(Single.just(true))
        viewModel.discoverNewTokens()
        viewModel.discoverNewTokens()
        viewModel.discoverNewTokens()
        verify(transactionRepository, times(1)).getTokenBalance()
    }

    @Test
    fun `Remove value error`() {
        val error = Throwable("error")
        whenever(accountManager.hideAccount(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(
            Completable.error(error)
        )
        whenever(walletConnectRepository.killAllAccountSessions(any(), any())).thenReturn(Completable.complete())
        whenever(accountManager.toChecksumAddress(any(), anyOrNull())).thenReturn("address")
        whenever(accountManager.rawAccounts).thenReturn(listOf(Account(1)))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.hideAccount(Account(1))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `Remove value success`() {
        whenever(accountManager.hideAccount(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(walletConnectRepository.killAllAccountSessions(any(), any())).thenReturn(Completable.complete())
        whenever(accountManager.toChecksumAddress(any(), anyOrNull())).thenReturn("address")
        whenever(accountManager.rawAccounts).thenReturn(listOf(Account(1)))
        viewModel.accountHideLiveData.observeForever(accountRemoveObserver)
        viewModel.hideAccount(Account(1))
        accountRemoveCaptor.run {
            verify(accountRemoveObserver).onChanged(capture())
        }
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable("error")
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.error(error))
        whenever(safeAccountRepository.createSafeAccount(any())).thenReturn(Single.error(error))
        whenever(accountManager.createOrUnhideAccount(any())).thenReturn(Single.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.createSafeAccount(Account(id = 1, cryptoBalance = BigDecimal.ONE))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `create safe account when balance is 0`() {
        whenever(safeAccountRepository.createSafeAccount(any())).thenReturn(Single.just("address"))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.createSafeAccount(Account(id = 1, cryptoBalance = BigDecimal.ZERO))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
            firstValue.peekContent() == NoFunds
        }
    }

    @Test
    fun `get sessions and update accounts success`() {
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(
            Flowable.just(
                listOf(DappSessionV1(address = "address"))
            )
        )
        whenever(accountManager.toChecksumAddress(any(), anyOrNull())).thenReturn("address")
        viewModel.dappSessions.observeForever(dappSessionObserver)
        viewModel.getSessions(accounts)
        dappSessionCaptor.run {
            verify(dappSessionObserver).onChanged(capture())
            firstValue.find { data -> data.address == "address" && data.chainId == 1 }?.count == 1
        }
    }

    @Test
    fun `get sessions and update accounts error`() {
        val error = Throwable()
        whenever(walletConnectRepository.getSessionsFlowable()).thenReturn(Flowable.error(error))
        whenever(accountManager.toChecksumAddress(any(), anyOrNull())).thenReturn("address")
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
            val erc20Token = ERCToken(1, address = "0xC00KiE", decimals = "2", type = TokenType.ERC20)
            whenever(settings.getTokenVisibility(any(), any())).thenReturn(false, false, true, true, null)
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ONE)) shouldBeEqualTo false
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ZERO)) shouldBeEqualTo false
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ONE)) shouldBeEqualTo true
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ZERO)) shouldBeEqualTo true
            viewModel.isTokenVisible("", AccountToken(erc20Token, BigDecimal.ONE)) shouldBeEqualTo null
            verify(settings, times(5)).getTokenVisibility(any(), any())
        }
    }

    @Test
    fun `get cached tokens when no account tokens test`() {
        val token = ERCToken(
            1,
            address = "tokenAddress",
            name = "cachedToken",
            accountAddress = "address1",
            decimals = "9",
            type = TokenType.ERC20
        )
        whenever(accountManager.cachedTokens).thenReturn(
            mapOf(
                1 to listOf(
                   token
                )
            )
        )
        viewModel.tokenVisibilitySettings = mock()
        with(viewModel.tokenVisibilitySettings) {
            whenever(getTokenVisibility("address1", "tokenAddress")).thenReturn(true)
        }
        val account = Account(1, address = "address1", chainId = 1, accountTokens = mutableListOf(AccountToken(rawBalance = BigDecimal.TEN,token = token)))
        val result = viewModel.getTokens(account)
        result[0].token.name shouldBeEqualTo "cachedToken"
    }

    @Test
    fun `get account tokens test`() {
        whenever(accountManager.cachedTokens).thenReturn(
            mapOf(
                1 to listOf(
                    ERCToken(
                        1,
                        name = "cachedToken",
                        accountAddress = "address1",
                        type = TokenType.ERC20
                    )
                )
            )
        )
        val account = Account(
            1,
            address = "address1",
            chainId = 1,
            accountTokens = mutableListOf(
                AccountToken(
                    rawBalance = BigDecimal.TEN,
                    tokenPrice = 2.0,
                    token = ERCToken(
                        1,
                        name = "cachedToken1",
                        accountAddress = "address1",
                        address = "heh2",
                        decimals = "2",
                        type = TokenType.ERC20
                    )
                ),
                AccountToken(
                    tokenPrice = 3.0,
                    rawBalance = BigDecimal.TEN,
                    token = ERCToken(
                        1,
                        name = "cachedToken2",
                        accountAddress = "address1",
                        address = "hehe1",
                        decimals = "2",
                        type = TokenType.ERC20
                    )
                )
            )
        )
        viewModel.tokenVisibilitySettings = mock()
        with(viewModel.tokenVisibilitySettings) {
            whenever(getTokenVisibility("address1", "hehe1")).thenReturn(true)
            whenever(getTokenVisibility("address1", "heh2")).thenReturn(true)
        }
        val result = viewModel.getTokens(account)
        result[0].token.name shouldBeEqualTo "cachedToken2"
        result[1].token.name shouldBeEqualTo "cachedToken1"
    }

    @Test
    fun `check creating new account flow`() {
        val error = Throwable("error")
        NetworkManager.initialize(listOf(Network(chainId = 3, httpRpc = "some_rpc")))
        whenever(accountManager.createOrUnhideAccount(any())).thenReturn(
            Single.just("Cookie Account"),
            Single.error(error)
        )
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.run {
            loadingLiveData.observeForever(loadingObserver)
            errorLiveData.observeForever(errorObserver)
        }
    }

    @Test
    fun `change account name flow`() {
        val error = Throwable("error")
        whenever(accountManager.changeAccountName(any(), any())).thenReturn(
            Completable.complete(),
            Completable.error(error)
        )
        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            changeAccountName(Account(1), "new name")
            changeAccountName(Account(1), "new name")
        }
        verify(errorObserver).onChanged(errorCaptor.capture())
    }

    @Test
    fun `update session count test`() {
        val accounts = listOf(
            Account(1, address = "address1", chainId = 1),
            Account(2, address = "address2", chainId = 2)
        )
        whenever(accountManager.activeAccounts).thenReturn(accounts)
        val lis = listOf(DappSessionData("address1", 1, 2), DappSessionData("address2", 2, 3))
        viewModel.updateSessionCount(lis) {}

        accounts[0].dappSessionCount shouldBeEqualTo 2
        accounts[1].dappSessionCount shouldBeEqualTo 3
    }

    @Test
    fun `show pending account test test`() {
        NetworkManager.initialize(listOf(Network(chainId = 99, httpRpc = "url")))
        val accounts = listOf(
            Account(1, address = "address1", chainId = 99),
            Account(2, address = "address2", chainId = 99)
        )
        whenever(accountManager.rawAccounts).thenReturn(accounts)
        viewModel.showPendingAccount(1, 99, areMainNetsEnabled = false, isPending = true) {}

        accounts[0].isPending shouldBeEqualTo true
        accounts[1].isPending shouldBeEqualTo false
    }

    @Test
    fun `stop pending accounts test`() {
        val accounts = listOf(
            Account(1, address = "address1", chainId = 99, isPending = true),
            Account(2, address = "address2", chainId = 99, isPending = true)
        )
        whenever(accountManager.rawAccounts).thenReturn(accounts)
        viewModel.stopPendingAccounts()
        accounts.all {
            it.isPending shouldBeEqualTo false
        }
    }
}