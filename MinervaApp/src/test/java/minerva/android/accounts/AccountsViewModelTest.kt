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
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.provider.CurrentTimeProvider
import minerva.android.walletmanager.repository.transaction.TransactionRepository
import minerva.android.walletmanager.smartContract.SmartContractRepository
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class AccountsViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val smartContractRepository: SmartContractRepository = mock()
    private val accountManager: AccountManager = mock()
    private val transactionRepository: TransactionRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val timeProvider: CurrentTimeProvider = mock()
    private lateinit var viewModel: AccountsViewModel

    private val balanceObserver: Observer<HashMap<String, Balance>> = mock()
    private val balanceCaptor: KArgumentCaptor<HashMap<String, Balance>> = argumentCaptor()

    private val assetsBalanceObserver: Observer<Map<String, List<AccountAsset>>> = mock()
    private val assetsBalanceCaptor: KArgumentCaptor<Map<String, List<AccountAsset>>> = argumentCaptor()

    private val noFundsObserver: Observer<Event<Unit>> = mock()
    private val noFundsCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

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
            localStorage,
            timeProvider
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
        viewModel.refreshBalancesErrorLiveData.observeForever(refreshBalancesErrorObserver)
        viewModel.refreshBalances()
        refreshBalancesErrorCaptor.run {
            verify(refreshBalancesErrorObserver).onChanged(capture())
            firstValue.peekContent() == ErrorCode.BALANCE_ERROR
        }
    }

    @Test
    fun `get assets balance success test`() {
        whenever(transactionRepository.refreshAssetBalance()).thenReturn(
            Single.just(
                mapOf(
                    Pair(
                        "test",
                        listOf(AccountAsset(Asset("name")))
                    )
                )
            )
        )
        viewModel.accountAssetBalanceLiveData.observeForever(assetsBalanceObserver)
        viewModel.refreshAssetBalance()
        assetsBalanceCaptor.run {
            verify(assetsBalanceObserver).onChanged(capture())
            (firstValue["test"] ?: error(""))[0].asset.name shouldBe "name"
        }
    }

    @Test
    fun `get assets balance error test`() {
        val error = Throwable()
        whenever(transactionRepository.refreshAssetBalance()).thenReturn(Single.error(error))
        viewModel.refreshBalancesErrorLiveData.observeForever(refreshBalancesErrorObserver)
        viewModel.refreshAssetBalance()
        refreshBalancesErrorCaptor.run {
            verify(refreshBalancesErrorObserver).onChanged(capture())
            firstValue.peekContent() == ErrorCode.ASSET_BALANCE_ERROR
        }
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
    fun `check that last free ATS was at least 24 hours (86400000 mills) ago`() {
        NetworkManager.initialize(networks)
        whenever(timeProvider.currentTimeMills()).thenReturn(1610120569428)
        timeProvider.currentTimeMills().let { time ->
            whenever(localStorage.getLastFreeATSTimestamp()).thenReturn(
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
    fun `adding free ATS correct` () {
        NetworkManager.initialize(networks)
        whenever(transactionRepository.getFreeATS(any())).thenReturn(Completable.complete())
        viewModel.addAtsToken(accounts, "nope")
        verify(localStorage, times(1)).saveFreeATSTimestamp(any())
    }

    @Test
    fun `adding free ATS error` () {
        NetworkManager.initialize(networks)
        viewModel.errorLiveData.observeForever(errorObserver)
        whenever(transactionRepository.getFreeATS(any())).thenReturn(Completable.error(Throwable("Some error")))
        viewModel.addAtsToken(accounts, "nope")
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `missing account for adding free ATS test error` () {
        viewModel.errorLiveData.observeForever(errorObserver)
        whenever(localStorage.getLastFreeATSTimestamp()).thenReturn(0)
        viewModel.addAtsToken(listOf(), "nope")
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    private val accounts = listOf(
        Account(1, network = Network(short = "second_network")),
        Account(2, network = Network(short = "first_network")),
        Account(3, network = Network(short = "first_network")),
        Account(4, network = Network(short = "some_other_network"))
    )

    private val accountsWithoutPrimaryAccount = listOf(
        Account(1, network = Network(short = "second_network")),
        Account(4, network = Network(short = "some_other_network"))
    )

    private val networks = listOf(
        Network(httpRpc = "some_rpc", short = "first_network"),
        Network(httpRpc = "some_rpc", short = "second_network"),
        Network(httpRpc = "some_rpc", short = "some_other_network")
    )
}