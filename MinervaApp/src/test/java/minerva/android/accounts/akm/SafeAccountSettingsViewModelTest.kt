package minerva.android.accounts.akm

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.smartContract.SmartContractRepository
import org.junit.Test

class SafeAccountSettingsViewModelTest : BaseViewModelTest() {

    private val accountManager: AccountManager = mock()
    private val smartContractRepository: SmartContractRepository = mock()

    private val ownersObserver: Observer<List<String>> = mock()
    private val kArgumentCaptor: KArgumentCaptor<List<String>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    private val addOwnerObserver: Observer<List<String>> = mock()

    private val viewModel: SafeAccountSettingsViewModel =
        SafeAccountSettingsViewModel(accountManager, smartContractRepository)

    private val mockValue = Account(0).apply {
        contractAddress = "0x123"
        owners = listOf("0x456")
    }

    @Test
    fun `load updated owner list`() {
        whenever(accountManager.loadAccount(any())).thenReturn(mockValue)
        whenever(smartContractRepository.getSafeAccountOwners(any(), any(), any(), any())).thenReturn(Single.just(listOf("0x456", "0x789")))
        viewModel.ownersLiveData.observeForever(ownersObserver)
        viewModel.loadAccount(0)
        kArgumentCaptor.run {
            verify(ownersObserver, times(2)).onChanged(capture())
            firstValue[0] == "0x456"
        }
    }

    @Test
    fun `getting current owners`() {
        whenever(accountManager.loadAccount(any())).thenReturn(mockValue)
        whenever(smartContractRepository.getSafeAccountOwners(any(), any(), any(), any())).thenReturn(Single.just(listOf("0x456", "0x789")))
        viewModel.run {
            loadAccount(0)
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
            account = mockValue
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
            account = mockValue
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
            account = mockValue
            addOwner("tom")
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }

    @Test
    fun `remove master owner error test`() {
        viewModel.run {
            account = Account(index = 0, owners = listOf("tom", "beata"))
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
            account = Account(index = 0, owners = listOf("tom", "beata"))
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
            account = Account(index = 0, owners = listOf("tom", "beata"))
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
            account = Account(index = 0, owners = listOf("tom", "beata"))
            removeOwner("tom")
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }
}