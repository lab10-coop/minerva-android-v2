package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.blockchainprovider.repository.contract.BlockchainContractRepository
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.smartContract.SmartContractRepositoryImpl
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.utils.DataProvider
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class SmartContractRepositoryTest {

    private val blockchainContractRepository: BlockchainContractRepository = mock()
    private val blockchainRepository: BlockchainRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val walletConfigManager: WalletConfigManager = mock()
    private val smartContractRepository = SmartContractRepositoryImpl(
        blockchainContractRepository,
        blockchainRepository,
        localStorage,
        walletConfigManager
    )
    val value = Account(0, owners = listOf("owner"))

    @Before
    fun setup(){
        whenever(walletConfigManager.getWalletConfig()) doReturn DataProvider.walletConfig
    }

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Test
    fun `create safe account success`() {
        whenever(blockchainContractRepository.deployGnosisSafeContract(any(), any(), any())).thenReturn(Single.just("address"))
        smartContractRepository.createSafeAccount(Account(index = 1, cryptoBalance = BigDecimal.ONE)).test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it == "address"
            }
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable()
        whenever(blockchainContractRepository.deployGnosisSafeContract(any(), any(), any())).thenReturn(Single.error(error))
        smartContractRepository.createSafeAccount(Account(index = 1, cryptoBalance = BigDecimal.ONE)).test()
            .assertError(error)
    }

    @Test
    fun `transfer native coin success`() {
        whenever(blockchainContractRepository.transferNativeCoin(any(), any())) doReturn Completable.complete()
        whenever(blockchainRepository.reverseResolveENS(any())) doReturn Single.just("tom")
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractRepository.transferNativeCoin("network", Transaction())
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `transfer native coin error`() {
        val error = Throwable()
        whenever(blockchainContractRepository.transferNativeCoin(any(), any())) doReturn Completable.error(error)
        whenever(blockchainRepository.reverseResolveENS(any())) doReturn Single.error(error)
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractRepository.transferNativeCoin("network", Transaction())
            .test()
            .assertError(error)
    }

    @Test
    fun `get safe account owners success`() {
        whenever(blockchainContractRepository.getGnosisSafeOwners(any(), any(), any())) doReturn Single.just(listOf("owner"))
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.just(listOf("owner"))
        smartContractRepository.getSafeAccountOwners("123", "ETH", "456", value)
            .test()
            .assertNoErrors()
    }

    @Test
    fun `get safe account owners error`() {
        val error = Throwable()
        whenever(blockchainContractRepository.getGnosisSafeOwners(any(), any(), any())) doReturn Single.error(error)
        smartContractRepository.getSafeAccountOwners("123", "ETH", "456",value)
            .test()
            .assertError(error)
    }

    @Test
    fun `add safe account owners success`() {
        whenever(blockchainContractRepository.addSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.complete()
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.complete()
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.just(listOf("test"))
        smartContractRepository.addSafeAccountOwner("owner", "123", "eth", "567", value)
            .test()
            .assertComplete()
            .assertValue {
                it[0] == "test"
            }
    }

    @Test
    fun `add safe account owners error`() {
        val error = Throwable()
        whenever(blockchainContractRepository.addSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.error(error)
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.error(error)
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.error(error)
        smartContractRepository.addSafeAccountOwner("owner", "123", "eth", "567", value)
            .test()
            .assertError(error)
    }

    @Test
    fun `remove safe account owners success`() {
        whenever(blockchainContractRepository.removeSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.complete()
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.complete()
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.just(listOf("test"))
        smartContractRepository.removeSafeAccountOwner("owner", "123", "eth", "567", value)
            .test()
            .assertComplete()
    }

    @Test
    fun `remove safe account owners error`() {
        val error = Throwable()
        whenever(blockchainContractRepository.removeSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.error(error)
        whenever(walletConfigManager.updateWalletConfig(any())) doReturn Completable.error(error)
        whenever(walletConfigManager.updateSafeAccountOwners(any(), any())) doReturn Single.error(error)
        smartContractRepository.removeSafeAccountOwner("owner", "123", "eth", "567", value)
            .test()
            .assertError(error)
    }

    @Test
    fun `transfer erc20 token success`() {
        whenever(blockchainContractRepository.transferERC20Token(any(), any(), any())) doReturn Completable.complete()
        whenever(blockchainRepository.reverseResolveENS(any())) doReturn Single.just("tom")
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractRepository.transferERC20Token("network", Transaction(), "address")
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `transfer erc20 token error`() {
        val error = Throwable()
        whenever(blockchainContractRepository.transferERC20Token(any(), any(), any())) doReturn Completable.error(error)
        whenever(blockchainRepository.reverseResolveENS(any())) doReturn Single.error(error)
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractRepository.transferERC20Token("network", Transaction(), "address")
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
}