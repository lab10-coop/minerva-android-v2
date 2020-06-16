package minerva.android.values.akm

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.values.ValueManager
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.smartContract.SmartContractRepository
import org.junit.Test

class SafeAccountSettingsViewModelTest : BaseViewModelTest() {

    private val valueManager: ValueManager = mock()
    private val smartContractRepository: SmartContractRepository = mock()

    private val ownersObserver: Observer<List<String>> = mock()
    private val kArgumentCaptor: KArgumentCaptor<List<String>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val addOwnerObserver: Observer<List<String>> = mock()

    private val viewModel: SafeAccountSettingsViewModel =
        SafeAccountSettingsViewModel(valueManager, smartContractRepository)

    private val mockValue = Value(0).apply {
        contractAddress = "0x123"
        owners = listOf("0x456")
    }

    @Test
    fun `load updated owner list`() {
        whenever(valueManager.loadValue(any())).thenReturn(mockValue)
        whenever(smartContractRepository.getSafeAccountOwners(any(), any(), any(), any())).thenReturn(Single.just(listOf("0x456", "0x789")))
        viewModel.ownersLiveData.observeForever(ownersObserver)
        viewModel.loadValue(0)
        kArgumentCaptor.run {
            verify(ownersObserver, times(2)).onChanged(capture())
            firstValue[0] == "0x456"
        }
    }

    @Test
    fun `getting current owners`() {
        whenever(valueManager.loadValue(any())).thenReturn(mockValue)
        whenever(smartContractRepository.getSafeAccountOwners(any(), any(), any(), any())).thenReturn(Single.just(listOf("0x456", "0x789")))
        viewModel.run {
            loadValue(0)
            getOwners("0x303", "", "")
            ownersLiveData.observeForever(addOwnerObserver)
        }
        kArgumentCaptor.run {
            verify(addOwnerObserver, times(1)).onChanged(capture())
        }
    }

    @Test
    fun `add already added owner error test`() {
        viewModel.run {
            value = mockValue
            errorLiveData.observeForever(errorObserver)
            addOwner("0x456")
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `add owner success test`() {
        whenever(smartContractRepository.addSafeAccountOwner(any(), any(), any(), any(), any())) doReturn Single.just(listOf("tom"))
        viewModel.run {
            ownersLiveData.observeForever(addOwnerObserver)
            value = mockValue
            addOwner("tom")
        }
        kArgumentCaptor.run {
            verify(addOwnerObserver, times(1)).onChanged(capture())
        }
    }

    @Test
    fun `add owner error test`() {
        val error = Throwable()
        whenever(smartContractRepository.addSafeAccountOwner(any(), any(), any(), any(), any())) doReturn Single.error(error)
        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            value = mockValue
            addOwner("tom")
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    @Test
    fun `remove master owner error test`() {
        viewModel.run {
            value = Value(index = 0, owners = listOf("tom", "beata"))
            errorLiveData.observeForever(errorObserver)
            removeOwner("beata")
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `remove not existing owner error test`() {
        viewModel.run {
            value = Value(index = 0, owners = listOf("tom", "beata"))
            errorLiveData.observeForever(errorObserver)
            removeOwner("papiez")
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }

    @Test
    fun `remove owner success test`() {
        whenever(smartContractRepository.removeSafeAccountOwner(any(), any(), any(), any(), any())) doReturn Single.just(listOf("tom", "beata"))
        viewModel.run {
            ownersLiveData.observeForever(ownersObserver)
            value = Value(index = 0, owners = listOf("tom", "beata"))
            removeOwner("tom")
        }
        kArgumentCaptor.run {
            verify(ownersObserver, times(1)).onChanged(capture())
        }
    }

    @Test
    fun `remove owner success error`() {
        val error = Throwable()
        whenever(smartContractRepository.removeSafeAccountOwner(any(), any(), any(), any(), any())) doReturn Single.error(error)
        viewModel.run {
            errorLiveData.observeForever(errorObserver)
            value = Value(index = 0, owners = listOf("tom", "beata"))
            removeOwner("tom")
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }
}