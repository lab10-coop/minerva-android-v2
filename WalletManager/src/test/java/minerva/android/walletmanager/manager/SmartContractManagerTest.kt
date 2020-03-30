package minerva.android.walletmanager.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import minerva.android.blockchainprovider.repository.contract.SmartContractRepository
import minerva.android.walletmanager.model.Value
import org.amshove.kluent.mock
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class SmartContractManagerTest {

    private val smartContractRepository: SmartContractRepository = mock()
    private val smartContractManager = SmartContractManagerImpl(smartContractRepository, mock(), mock())

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
}