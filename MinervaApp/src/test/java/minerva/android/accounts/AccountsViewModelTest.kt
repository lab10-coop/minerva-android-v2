package minerva.android.accounts

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.accounts.transaction.fragment.*
import minerva.android.accounts.transaction.model.DappSessionData
import minerva.android.kotlinUtils.event.Event
import minerva.android.mock.*
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.DefaultWalletConfigIndexes
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.minervaprimitives.account.CoinBalance
import minerva.android.walletmanager.model.minervaprimitives.account.TokenBalance
import minerva.android.walletmanager.model.token.AccountToken
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.model.transactions.Balance
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.repository.smartContract.SmartContractRepository
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.repository.walletconnect.WalletConnectRepository
import minerva.android.walletmanager.utils.logger.Logger
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AccountsViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val smartContractRepository: SmartContractRepository = mock()
    private val accountManager: AccountManager = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val walletConnectRepository: WalletConnectRepository = mock()
    private val logger: Logger = mock()
    private lateinit var viewModel: AccountsViewModel

    private val balanceObserver: Observer<List<CoinBalance>> = mock()
    private val balanceCaptor: KArgumentCaptor<List<CoinBalance>> = argumentCaptor()

    private val tokensBalanceObserver: Observer<Unit> = mock()
    private val tokensBalanceCaptor: KArgumentCaptor<Unit> = argumentCaptor()

    private val dappSessionObserver: Observer<List<DappSessionData>> = mock()
    private val dappSessionCaptor: KArgumentCaptor<List<DappSessionData>> = argumentCaptor()

    private val errorObserver: Observer<Event<AccountsErrorState>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<AccountsErrorState>> = argumentCaptor()

    private val accountRemoveObserver: Observer<Event<Unit>> = mock()
    private val accountRemoveCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val loadingObserver: Observer<Event<Boolean>> = mock()
    private val loadingCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    @Before
    fun initViewModel() {
        viewModel = AccountsViewModel(
            accountManager,
            walletActionsRepository,
            smartContractRepository,
            transactionRepository,
            walletConnectRepository,
            logger
        )
    }

    @Test
    fun `are pending transactions empty`() {
        whenever(transactionRepository.getPendingAccounts()).thenReturn(emptyList())
        val result = viewModel.arePendingAccountsEmpty
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
        whenever(transactionRepository.refreshCoinBalances()).thenReturn(
            Single.just(listOf(CoinBalance(1, "123", Balance(cryptoBalance = BigDecimal.ONE, fiatBalance = BigDecimal.TEN))))
        )
        viewModel.balanceLiveData.observeForever(balanceObserver)
        viewModel.refreshCoinBalances()
        balanceCaptor.run {
            verify(balanceObserver).onChanged(capture())
            firstValue.find { balance -> balance.chainId == 1 && balance.address == "123" }?.balance?.cryptoBalance == BigDecimal.ONE
        }
    }

    @Test
    fun `refresh balances error`() {
        val error = Throwable()
        whenever(walletConnectRepository.getSessionsFlowable())
            .thenReturn(Flowable.just(listOf(DappSession(address = "address"))))
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
        whenever(accountManager.getAllAccounts()).thenReturn(accounts)
        whenever(transactionRepository.refreshCoinBalances()).thenReturn(Single.error(error))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.refreshCoinBalances()
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
            firstValue.peekContent() == RefreshCoinBalancesError
        }
    }

    @Test
    fun `get tokens balance success when tagged tokens are not empty test`() {
        whenever(transactionRepository.getTaggedTokensUpdate())
            .thenReturn(Flowable.just(listOf(ERC20Token(1, "token"))))
        whenever(transactionRepository.refreshTokensBalances())
            .thenReturn(Single.just(listOf(TokenBalance(1, "test", listOf(AccountToken(ERC20Token(1, "name")))))))
        viewModel.refreshTokensBalances()
        viewModel.tokenBalanceLiveData.observeForever(tokensBalanceObserver)
        tokensBalanceCaptor.run {
            verify(tokensBalanceObserver).onChanged(capture())
        }
    }

    @Test
    fun `get tokens balance success when tagged tokens are empty test`() {
        whenever(transactionRepository.getTaggedTokensUpdate()).thenReturn(Flowable.just(emptyList()))
        whenever(transactionRepository.refreshTokensBalances())
            .thenReturn(Single.just(listOf(TokenBalance(1, "test", listOf(AccountToken(ERC20Token(1, "name")))))))
        viewModel.refreshTokensBalances()
        viewModel.tokenBalanceLiveData.observeForever(tokensBalanceObserver)
        tokensBalanceCaptor.run {
            verifyNoMoreInteractions(tokensBalanceObserver)
        }
    }

    @Test
    fun `get tokens balance success when tagged tokens are not empty and check tokens visibility test`() {
        whenever(transactionRepository.getTaggedTokensUpdate())
            .thenReturn(
                Flowable.just(listOf(ERC20Token(1, "name1", address = "tokenAddress1", tag = "tag")))
            )
        whenever(transactionRepository.refreshTokensBalances())
            .thenReturn(
                Single.just(
                    listOf(
                        TokenBalance(1, "privateKey1", accountTokensForPrivateKey1),
                        TokenBalance(2, "privateKey2", accountTokensForPrivateKey2)
                    )
                )
            )

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
        viewModel.tokenBalanceLiveData.observeForever(tokensBalanceObserver)
        tokensBalanceCaptor.run {
            verify(tokensBalanceObserver).onChanged(capture())
        }
    }

    @Test
    fun `get tokens balance error test`() {
        val error = Throwable()
        whenever(transactionRepository.refreshTokensBalances()).thenReturn(Single.error(error))
        whenever(transactionRepository.getTaggedTokensUpdate()).thenReturn(Flowable.just(listOf(ERC20Token(1, "token"))))
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.refreshTokensBalances()
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
            firstValue.peekContent() == RefreshTokenBalancesError
        }
    }

    @Test
    fun `get tokens list test`() {
        whenever(transactionRepository.discoverNewTokens()).thenReturn(
            Single.just(true),
            Single.just(false),
            Single.error(Throwable("Refresh tokens list error"))
        )
        whenever(transactionRepository.getTaggedTokensUpdate())
            .thenReturn(Flowable.just(listOf(ERC20Token(1, "token"))))
        whenever(walletConnectRepository.getSessionsFlowable())
            .thenReturn(Flowable.just(listOf(DappSession(address = "address"))))
        whenever(transactionRepository.refreshTokensBalances()).thenReturn(Single.just(emptyList()))

        viewModel.discoverNewTokens()
        viewModel.discoverNewTokens()
        viewModel.discoverNewTokens()
        verify(transactionRepository, times(1)).refreshTokensBalances()
    }

    @Test
    fun `Remove value error`() {
        val error = Throwable("error")
        whenever(accountManager.hideAccount(any())).thenReturn(Completable.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(
            Completable.error(error)
        )
        whenever(walletConnectRepository.killAllAccountSessions(any())).thenReturn(Completable.complete())
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.hideAccount(Account(1, "test"))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `Remove value success`() {
        whenever(accountManager.hideAccount(any())).thenReturn(Completable.complete())
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        whenever(walletConnectRepository.killAllAccountSessions(any())).thenReturn(Completable.complete())
        whenever(accountManager.toChecksumAddress(any())).thenReturn("address")
        viewModel.accountHideLiveData.observeForever(accountRemoveObserver)
        viewModel.hideAccount(Account(1, "test"))
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
        viewModel.errorLiveData.observeForever(errorObserver)
        viewModel.createSafeAccount(Account(id = 1, cryptoBalance = BigDecimal.ZERO))
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
            firstValue.peekContent() == NoFunds
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
        whenever(accountManager.getLastFreeATSTimestamp()).thenReturn(0L)
        whenever(accountManager.currentTimeMills()).thenReturn(TimeUnit.HOURS.toMillis(24L) + 3003)
        whenever(accountManager.activeAccounts)
            .thenReturn(
                listOf(
                    Account(
                        1, chainId =
                        NetworkManager.networks[DefaultWalletConfigIndexes.FIRST_DEFAULT_TEST_NETWORK_INDEX].chainId
                    )
                )
            )
        viewModel.apply {
            addAtsToken()
        }
        verify(accountManager, times(1)).saveFreeATSTimestamp()
    }

    @Test
    fun `adding free ATS error`() {
        NetworkManager.initialize(networks)
        whenever(transactionRepository.getFreeATS(any())).thenReturn(Completable.error(Throwable("Some error")))
        whenever(accountManager.getLastFreeATSTimestamp()).thenReturn(0L)
        whenever(accountManager.currentTimeMills()).thenReturn(TimeUnit.HOURS.toMillis(24L) + 3003)
        whenever(accountManager.activeAccounts)
            .thenReturn(
                listOf(
                    Account(
                        1, chainId =
                        NetworkManager.networks[DefaultWalletConfigIndexes.FIRST_DEFAULT_TEST_NETWORK_INDEX].chainId
                    )
                )
            )
        viewModel.apply {
            errorLiveData.observeForever(errorObserver)
            addAtsToken()
        }

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
            firstValue.find { data -> data.address == "address" && data.chainId == 1 }?.count == 1
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

    @Test
    fun `get cached tokens when no account tokens test`() {
        whenever(accountManager.cachedTokens).thenReturn(
            mapOf(
                1 to listOf(
                    ERC20Token(
                        1,
                        address = "tokenAddress",
                        name = "cachedToken",
                        accountAddress = "address1"
                    )
                )
            )
        )
        viewModel.tokenVisibilitySettings = mock()
        with(viewModel.tokenVisibilitySettings) {
            whenever(getTokenVisibility("address1", "tokenAddress")).thenReturn(true)
        }
        val account = Account(1, address = "address1", chainId = 1)
        val result = viewModel.getTokens(account)
        result[0].name shouldBeEqualTo "cachedToken"
    }

    @Test
    fun `get account tokens test`() {
        whenever(accountManager.cachedTokens).thenReturn(
            mapOf(
                1 to listOf(
                    ERC20Token(
                        1,
                        name = "cachedToken",
                        accountAddress = "address1"
                    )
                )
            )
        )
        val account = Account(
            1,
            address = "address1",
            chainId = 1,
            accountTokens = listOf(
                AccountToken(
                    tokenPrice = 2.0,
                    token = ERC20Token(1, name = "cachedToken1", accountAddress = "address1")
                ),
                AccountToken(tokenPrice = 3.0, token = ERC20Token(1, name = "cachedToken2", accountAddress = "address1"))
            )
        )
        val result = viewModel.getTokens(account)
        result[0].name shouldBeEqualTo "cachedToken1"
        result[1].name shouldBeEqualTo "cachedToken2"
    }

    @Test
    fun `check creating new account flow`() {
        val error = Throwable("error")
        NetworkManager.initialize(listOf(Network(chainId = 3, httpRpc = "some_rpc")))
        whenever(accountManager.createRegularAccount(any())).thenReturn(Single.just("Cookie Account"), Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.run {
            loadingLiveData.observeForever(loadingObserver)
            errorLiveData.observeForever(errorObserver)
            createNewAccount(3)
            createNewAccount(3)
        }

        verify(loadingObserver, times(4)).onChanged(loadingCaptor.capture())
        verify(errorObserver).onChanged(errorCaptor.capture())
    }

    @Test
    fun `change account name flow`() {
        val error = Throwable("error")
        whenever(accountManager.changeAccountName(any(), any())).thenReturn(Completable.complete(), Completable.error(error))
        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            changeAccountName(Account(1), "new name")
            changeAccountName(Account(1), "new name")
        }
        verify(errorObserver).onChanged(errorCaptor.capture())
    }
}