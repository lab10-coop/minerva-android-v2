package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.ens.ENSRepository
import minerva.android.blockchainprovider.repository.erc20.ERC20TokenRepository
import minerva.android.blockchainprovider.repository.safeAccount.BlockchainSafeAccountRepository
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.defs.ChainId.Companion.ATS_TAU
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.transactions.Transaction
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.smartContract.SafeAccountRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.MockDataProvider
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

class SafeAccountRepositoryTest {

    private val blockchainSafeAccountRepository: BlockchainSafeAccountRepository = mock()
    private val erC20TokenRepository: ERC20TokenRepository = mock()
    private val ensRepository: ENSRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val walletConfigManager: WalletConfigManager = mock()
    private val smartContractRepository = SafeAccountRepositoryImpl(
        blockchainSafeAccountRepository,
        erC20TokenRepository,
        ensRepository,
        localStorage,
        walletConfigManager
    )
    val value = Account(0, owners = listOf("owner"))

    @Before
    fun setup() {
        whenever(walletConfigManager.getWalletConfig()) doReturn MockDataProvider.walletConfig
    }

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Test
    fun `create safe account success`() {
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(blockchainSafeAccountRepository.deployGnosisSafeContract(any(), any(), any())).thenReturn(Single.just("address"))
        smartContractRepository.createSafeAccount(Account(id = 1, cryptoBalance = BigDecimal.ONE, chainId = 4)).test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it == "address"
            }
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable()
        NetworkManager.initialize(MockDataProvider.networks)
        whenever(blockchainSafeAccountRepository.deployGnosisSafeContract(any(), any(), any())).thenReturn(Single.error(error))
        smartContractRepository.createSafeAccount(Account(id = 1, cryptoBalance = BigDecimal.ONE, chainId = 4)).test()
            .assertError(error)
    }

    @Test
    fun `transfer native coin success`() {
        whenever(blockchainSafeAccountRepository.transferNativeCoin(any(), any())) doReturn Completable.complete()
        whenever(ensRepository.reverseResolveENS(any())) doReturn Single.just("tom")
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractRepository.transferNativeCoin(0, Transaction())
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `transfer native coin error`() {
        val error = Throwable()
        whenever(blockchainSafeAccountRepository.transferNativeCoin(any(), any())) doReturn Completable.error(error)
        whenever(ensRepository.reverseResolveENS(any())) doReturn Single.error(error)
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractRepository.transferNativeCoin(0, Transaction())
            .test()
            .assertError(error)
    }

    @Test
    fun `get safe account owners success`() {
        whenever(blockchainSafeAccountRepository.getGnosisSafeOwners(any(), any(), any())) doReturn Single.just(listOf("owner"))
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.just(listOf("owner"))
        smartContractRepository.getSafeAccountOwners("123", 0, "456", value)
            .test()
            .assertNoErrors()
    }

    @Test
    fun `get safe account owners error`() {
        val error = Throwable()
        whenever(blockchainSafeAccountRepository.getGnosisSafeOwners(any(), any(), any())) doReturn Single.error(error)
        smartContractRepository.getSafeAccountOwners("123", 0, "456", value)
            .test()
            .assertError(error)
    }

    @Test
    fun `add safe account owners success`() {
        whenever(blockchainSafeAccountRepository.addSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.complete()
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.complete()
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.just(listOf("test"))
        smartContractRepository.addSafeAccountOwner("owner", "123", 0, "567", value)
            .test()
            .assertComplete()
            .assertValue {
                it[0] == "test"
            }
    }

    @Test
    fun `add safe account owners error`() {
        val error = Throwable()
        whenever(
            blockchainSafeAccountRepository.addSafeAccountOwner(
                any(),
                any(),
                any(),
                any()
            )
        ) doReturn Completable.error(error)
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.error(error)
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.error(error)
        smartContractRepository.addSafeAccountOwner("owner", "123", 0, "567", value)
            .test()
            .assertError(error)
    }

    @Test
    fun `remove safe account owners success`() {
        whenever(
            blockchainSafeAccountRepository.removeSafeAccountOwner(
                any(),
                any(),
                any(),
                any()
            )
        ) doReturn Completable.complete()
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.complete()
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.just(listOf("test"))
        smartContractRepository.removeSafeAccountOwner("owner", "123", 0, "567", value)
            .test()
            .assertComplete()
    }

    @Test
    fun `remove safe account owners error`() {
        val error = Throwable()
        whenever(blockchainSafeAccountRepository.removeSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.error(
            error
        )
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.error(error)
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.error(error)
        smartContractRepository.removeSafeAccountOwner("owner", "123", 0, "567", value)
            .test()
            .assertError(error)
    }

    @Test
    fun `transfer erc20 token success`() {
        whenever(blockchainSafeAccountRepository.transferERC20Token(any(), any(), any())) doReturn Completable.complete()
        whenever(ensRepository.reverseResolveENS(any())) doReturn Single.just("tom")
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractRepository.transferERC20Token(0, Transaction(), "address")
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `transfer erc20 token error`() {
        val error = Throwable()
        whenever(blockchainSafeAccountRepository.transferERC20Token(any(), any(), any())) doReturn Completable.error(error)
        whenever(ensRepository.reverseResolveENS(any())) doReturn Single.error(error)
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractRepository.transferERC20Token(0, Transaction(), "address")
            .test()
            .assertError(error)
    }

    @Test
    fun `get safe account master owner key test`() {
        val expected = Account(0, address = "address", privateKey = "key")
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(accounts = listOf(expected))
        whenever(walletConfigManager.getSafeAccountMasterOwnerPrivateKey(any())) doReturn "key"
        smartContractRepository.run {
            val result = getSafeAccountMasterOwnerPrivateKey("address")
            assertEquals(result, "key")
        }
    }

    @Test
    fun `get safe account master owner key error test`() {
        val expected = Account(0, address = "123", privateKey = "key")
        whenever(walletConfigManager.getWalletConfig()) doReturn WalletConfig(accounts = listOf(expected))
        whenever(walletConfigManager.getSafeAccountMasterOwnerPrivateKey(any())) doReturn ""
        smartContractRepository.run {
            val result = getSafeAccountMasterOwnerPrivateKey("address")
            assertEquals(result, "")
        }
    }

    @Test
    fun `get Token details`() {
        val error = Throwable("Some error")
        val name = "CookieToken"
        val symbol = "Cookie"
        val decimal = BigInteger.ONE
        NetworkManager.initialize(MockDataProvider.networks)
        (erC20TokenRepository).run {
            whenever(getERC20TokenName(any(), any(), any())).thenReturn(
                Observable.just(name),
                Observable.error(error),
                Observable.just(name),
                Observable.just(name)
            )
            whenever(getERC20TokenSymbol(any(), any(), any())).thenReturn(
                Observable.just(symbol),
                Observable.just(symbol),
                Observable.error(error),
                Observable.just(symbol)
            )
            whenever(getERC20TokenDecimals(any(), any(), any())).thenReturn(
                Observable.just(decimal),
                Observable.just(decimal),
                Observable.just(decimal),
                Observable.error(error)
            )
        }
        smartContractRepository.run {
            getERC20TokenDetails("privateKey", ATS_TAU, "address").test().assertComplete()
                .assertValue {
                    it.name == name
                    it.symbol == symbol
                    it.decimals == decimal.toString()
                }
            getERC20TokenDetails("privateKey", ATS_TAU, "address").test().assertError(error)
            getERC20TokenDetails("privateKey", ATS_TAU, "address").test().assertError(error)
            getERC20TokenDetails("privateKey", ATS_TAU, "address").test().assertError(error)
        }
    }
}