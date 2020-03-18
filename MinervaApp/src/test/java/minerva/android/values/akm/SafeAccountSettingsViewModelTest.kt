package minerva.android.values.akm

import androidx.lifecycle.Observer
import minerva.android.BaseViewModelTest
import minerva.android.walletmanager.manager.wallet.WalletManager
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.walletmanager.manager.SmartContractManager
import minerva.android.walletmanager.model.Value
import org.junit.Test

class SafeAccountSettingsViewModelTest: BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val smartContractManager: SmartContractManager = mock()

    private val ownersObserver: Observer<List<String>> = mock()
    private val kArgumentCaptor: KArgumentCaptor<List<String>> = argumentCaptor()

    private val addOwnerObserver: Observer<List<String>> = mock()


    private val viewModel: SafeAccountSettingsViewModel = SafeAccountSettingsViewModel(walletManager, smartContractManager)

    private val value = Value(0).apply {
        contractAddress = "0x123"
        owners = listOf("0x456")
    }

    @Test
    fun `load updated owner list`() {
        whenever(walletManager.loadValue(any())).thenReturn(value)
        whenever(smartContractManager.getSafeAccountOwners(any(), any(), any())).thenReturn(Single.just(listOf("0x456", "0x789")))
        viewModel.ownersLiveData.observeForever(ownersObserver)
        viewModel.loadValue(0)
        kArgumentCaptor.run {
            verify(ownersObserver, times(1)).onChanged(capture())
            firstValue[0] == "0x456"
        }
    }

    @Test
    fun `getting current owners`() {
        val updatedList = listOf("0x456", "0x789")
        whenever(walletManager.loadValue(any())).thenReturn(value)
        whenever(smartContractManager.getSafeAccountOwners(any(), any(), any())).thenReturn(Single.just(listOf("0x456", "0x789")))
        whenever(smartContractManager.getSafeAccountOwners(any(), any(), any())).thenReturn(Single.just(listOf("", "0x303")))
        whenever(walletManager.updateSafeAccountOwners(any(), any())).thenReturn(Single.just(updatedList + "0x303"))
        viewModel.loadValue(0)
        viewModel.getOwners("0x303", "", "")
        viewModel.ownersLiveData.observeForever(addOwnerObserver)
        kArgumentCaptor.run {
            verify(addOwnerObserver, times(1)).onChanged(capture())
        }
    }

}