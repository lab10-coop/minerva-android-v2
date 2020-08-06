package minerva.android.walletmanager.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.exchangemarketsprovider.api.BinanceApi
import com.exchangemarketsprovider.model.Market
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import minerva.android.blockchainprovider.model.TransactionCostPayload
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.repository.transaction.TransactionRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class TransactionRepositoryTest {

    private val walletConfigManager: WalletConfigManager = mock()
    private val blockchainRepository: BlockchainRepository = mock()
    private val binanaceApi: BinanceApi = mock()
    private val localStorage: LocalStorage = mock()
    private val repository = TransactionRepositoryImpl(blockchainRepository, walletConfigManager, binanaceApi, localStorage)

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Before
    fun setupRxSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        whenever(walletConfigManager.getWalletConfig()) doReturn  DataProvider.walletConfig
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed(_seed = "seed"))
    }

    @After
    fun destroyRxSchedulers() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `refresh balances test success`() {
        whenever(blockchainRepository.refreshBalances(any())).thenReturn(Single.just(listOf(Pair("address", BigDecimal.ONE))))
        whenever(binanaceApi.fetchExchangeRate(any())).thenReturn(Single.just(Market("ETHEUR", "12.21")))
        repository.refreshBalances().test()
            .assertComplete()
            .assertValue {
                it["address"]?.cryptoBalance == BigDecimal.ONE
            }
    }

    @Test
    fun `refresh balances test error`() {
        val error = Throwable()
        whenever(blockchainRepository.refreshBalances(any())).thenReturn(Single.error(error))
        whenever(binanaceApi.fetchExchangeRate(any())).thenReturn(Single.error(error))
        repository.refreshBalances().test()
            .assertError(error)
    }

    @Test
    fun `send transaction success with resolved ENS test`() {
        whenever(blockchainRepository.transferNativeCoin(any(), any())).thenReturn(Single.just("hash"))
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.just("didi.eth"))
        repository.transferNativeCoin("", Transaction("address", "privKey", "publicKey", BigDecimal.ONE, BigDecimal.ONE, BigInteger.ONE))
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction success with not resolved ENS test`() {
        whenever(blockchainRepository.transferNativeCoin(any(), any())).thenReturn(Single.just("hash"))
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.error(Throwable("No ENS")))
        repository.transferNativeCoin("", Transaction("address", "privKey", "publicKey", BigDecimal.ONE, BigDecimal.ONE, BigInteger.ONE))
            .test()
            .assertComplete()
    }

    @Test
    fun `send transaction error test`() {
        val error = Throwable()
        whenever(blockchainRepository.transferNativeCoin(any(), any())).thenReturn(Single.error(error))
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.error(Throwable()))
        repository.transferNativeCoin("", Transaction("address", "privKey", "publicKey", BigDecimal.ONE, BigDecimal.ONE, BigInteger.ONE))
            .test()
            .assertError(error)
    }

    @Test
    fun `calculate transaction cost success`() {
        whenever(blockchainRepository.calculateTransactionCost(any(), any())).thenReturn(BigDecimal.ONE)
        val result = repository.calculateTransactionCost(BigDecimal.ONE, BigInteger.ONE)
        result shouldBeEqualTo BigDecimal.ONE
    }

    @Test
    fun `get assets balances complete test`() {
        whenever(blockchainRepository.refreshAssetBalance(any(), any(), any(), any())).thenReturn(
            Observable.just(Pair("privateKey1", BigDecimal.TEN))
        )
        NetworkManager.initialize(listOf(Network(short = "tats1", url = "some"), Network(short = "rin", url = "some1")))
        repository.apply {
            refreshAssetBalance()
                .test()
                .assertComplete()
        }
    }

    @Test
    fun `get asset balances when values are empty and there are no assets needed test`() {
        val error = Throwable()
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(0, accounts = emptyList())
        whenever(blockchainRepository.refreshAssetBalance(any(), any(), any(), any())) doReturn Observable.error(error)
        whenever(walletConfigManager.masterSeed).thenReturn(MasterSeed())
        repository.apply {
            refreshAssetBalance()
                .test()
                .assertComplete()
                .assertValue {
                    it.isEmpty()
                }
        }
    }

    @Test
    fun `make ERC20 transfer with ENS resolved success test`() {
        whenever(blockchainRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.just("didi.eth"))
        repository.transferERC20Token("", Transaction())
            .test()
            .assertComplete()
    }

    @Test
    fun `make ERC20 transfer with not ENS resolved success test`() {
        whenever(blockchainRepository.transferERC20Token(any(), any())).thenReturn(Completable.complete())
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.error(Throwable()))
        repository.transferERC20Token("", Transaction()).test().assertComplete()
    }

    @Test
    fun `make ERC20 transfer error test`() {
        val error = Throwable()
        whenever(blockchainRepository.transferERC20Token(any(), any())).thenReturn(Completable.error(error))
        whenever(blockchainRepository.reverseResolveENS(any())).thenReturn(Single.just("didi.eth"))
        repository.transferERC20Token("", Transaction()).test().assertError(error)
    }

    @Test
    fun `resolve ens test`() {
        whenever(blockchainRepository.resolveENS(any())) doReturn Single.just("tom")
        repository.resolveENS("tom.eth")
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it == "tom"
            }
    }

    @Test
    fun `resolve ens error test`() {
        val error = Throwable()
        whenever(blockchainRepository.resolveENS(any())) doReturn Single.error(error)
        repository.resolveENS("tom.eth")
            .test()
            .assertError(error)
    }

    @Test
    fun `load recipients test`() {
        whenever(localStorage.loadRecipients()) doReturn listOf(Recipient(ensName = "tom"))
        val result = repository.loadRecipients()
        assertEquals(result, listOf(Recipient(ensName = "tom")))
    }

    @Test
    fun `get transfer costs test`() {
        whenever(blockchainRepository.getTransactionCosts(any(), any(), any())) doReturn TransactionCostPayload(
            BigDecimal.ONE,
            BigInteger.ONE,
            BigDecimal.ONE
        )
        val result = repository.getTransferCosts("network", 1)
        assertEquals(result, TransactionCost(BigDecimal.ONE, BigInteger.ONE, BigDecimal.ONE))
    }

    @Test
    fun `get value test`(){
        whenever(walletConfigManager.getAccount(any(), any())) doReturn Account(index = 2)
        val result = repository.getAccount(2, 2)
        assertEquals(result?.index, 2)
    }
}