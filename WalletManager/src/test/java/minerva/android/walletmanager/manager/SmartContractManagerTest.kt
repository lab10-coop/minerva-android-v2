package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.blockchain.BlockchainRepository
import minerva.android.blockchainprovider.repository.contract.SmartContractRepository
import minerva.android.walletmanager.model.Transaction
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.storage.LocalStorage
import org.amshove.kluent.mock
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class SmartContractManagerTest {

    private val smartContractRepository: SmartContractRepository = mock()
    private val blockchainRepository: BlockchainRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val smartContractManager = SmartContractManagerImpl(smartContractRepository, blockchainRepository, localStorage)

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Test
    fun `create safe account success`() {
        whenever(smartContractRepository.deployGnosisSafeContract(any(), any(), any())).thenReturn(Single.just("address"))
        smartContractManager.createSafeAccount(Value(index = 1, balance = BigDecimal.ONE)).test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it == "address"
            }
    }

    @Test
    fun `create safe account error`() {
        val error = Throwable()
        whenever(smartContractRepository.deployGnosisSafeContract(any(), any(), any())).thenReturn(Single.error(error))
        smartContractManager.createSafeAccount(Value(index = 1, balance = BigDecimal.ONE)).test()
            .assertError(error)
    }

    @Test
    fun `transfer native coin success`() {
        whenever(smartContractRepository.transferNativeCoin(any(), any())) doReturn Completable.complete()
        whenever(blockchainRepository.reverseResolveENS(any())) doReturn Single.just("tom")
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractManager.transferNativeCoin("network", Transaction())
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `transfer native coin error`() {
        val error = Throwable()
        whenever(smartContractRepository.transferNativeCoin(any(), any())) doReturn Completable.error(error)
        whenever(blockchainRepository.reverseResolveENS(any())) doReturn Single.error(error)
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractManager.transferNativeCoin("network", Transaction())
            .test()
            .assertError(error)
    }

    @Test
    fun `get safe account owners success`() {
        whenever(smartContractRepository.getGnosisSafeOwners(any(), any(), any())) doReturn Single.just(listOf("owner"))
        smartContractManager.getSafeAccountOwners("123", "ETH", "456")
            .test()
            .assertNoErrors()
            .assertValue {
                it[0] == "owner"
            }
    }

    @Test
    fun `get safe account owners error`() {
        val error = Throwable()
        whenever(smartContractRepository.getGnosisSafeOwners(any(), any(), any())) doReturn Single.error(error)
        smartContractManager.getSafeAccountOwners("123", "ETH", "456")
            .test()
            .assertError(error)
    }

    @Test
    fun `add safe account owners success`() {
        whenever(smartContractRepository.addSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.complete()
        smartContractManager.addSafeAccountOwner("owner", "123", "eth", "567")
            .test()
            .assertComplete()
    }

    @Test
    fun `add safe account owners error`() {
        val error = Throwable()
        whenever(smartContractRepository.addSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.error(error)
        smartContractManager.addSafeAccountOwner("owner", "123", "eth", "567")
            .test()
            .assertError(error)
    }

    @Test
    fun `remove safe account owners success`() {
        whenever(smartContractRepository.removeSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.complete()
        smartContractManager.removeSafeAccountOwner("owner", "123", "eth", "567")
            .test()
            .assertComplete()
    }

    @Test
    fun `remove safe account owners error`() {
        val error = Throwable()
        whenever(smartContractRepository.removeSafeAccountOwner(any(), any(), any(), any())) doReturn Completable.error(error)
        smartContractManager.removeSafeAccountOwner("owner", "123", "eth", "567")
            .test()
            .assertError(error)
    }

    @Test
    fun `transfer erc20 token success`() {
        whenever(smartContractRepository.transferERC20Token(any(), any(), any())) doReturn Completable.complete()
        whenever(blockchainRepository.reverseResolveENS(any())) doReturn Single.just("tom")
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractManager.transferERC20Token("network", Transaction(), "address")
            .test()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `transfer erc20 token error`() {
        val error = Throwable()
        whenever(smartContractRepository.transferERC20Token(any(), any(), any())) doReturn Completable.error(error)
        whenever(blockchainRepository.reverseResolveENS(any())) doReturn Single.error(error)
        doNothing().whenever(localStorage).saveRecipient(any())
        smartContractManager.transferERC20Token("network", Transaction(), "address")
            .test()
            .assertError(error)
    }
}