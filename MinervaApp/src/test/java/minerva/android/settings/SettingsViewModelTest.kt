package minerva.android.settings

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.storage.LocalStorage
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import kotlin.test.assertEquals

class SettingsViewModelTest : BaseViewModelTest() {

    private val masterSeedRepository: MasterSeedRepository = mock()
    private val localStorage: LocalStorage = mock()
    private val walletConfigManager: WalletConfigManager = mock()
    private val viewModel = SettingsViewModel(masterSeedRepository, localStorage, walletConfigManager)

    private val resetTokensObserver: Observer<Event<Result<Any>>> = mock()
    private val resetTokensCaptor: KArgumentCaptor<Event<Result<Any>>> = argumentCaptor()


    @Test
    fun `are main nets enabled returns true test`() {
        whenever(masterSeedRepository.areMainNetworksEnabled).thenReturn(true)
        val result = viewModel.areMainNetsEnabled
        assertEquals(true, result)
    }

    @Test
    fun `are main nets enabled returns false test`() {
        whenever(masterSeedRepository.areMainNetworksEnabled).thenReturn(false)
        val result = viewModel.areMainNetsEnabled
        assertEquals(false, result)
    }

    @Test
    fun `is mnemonic remembered returns true test`() {
        whenever(masterSeedRepository.isMnemonicRemembered()).thenReturn(true)
        val result = viewModel.isMnemonicRemembered
        assertEquals(result, true)
    }

    @Test
    fun `is mnemonic remembered returns false test`() {
        whenever(masterSeedRepository.isMnemonicRemembered()).thenReturn(false)
        val result = viewModel.isMnemonicRemembered
        assertEquals(result, false)
    }

    @Test
    fun `is synced returns true test`() {
        whenever(masterSeedRepository.isSynced).thenReturn(true)
        val result = viewModel.isSynced
        assertEquals(result, true)
    }

    @Test
    fun `is synced returns false test`() {
        whenever(masterSeedRepository.isSynced).thenReturn(false)
        val result = viewModel.isSynced
        assertEquals(result, false)
    }

    @Test
    fun `check getting current fiat index`() {
        val currencies = arrayOf(
            "EUR|Euro",
            "GBP|Pound Sterling",
            "USD|US Dollar",
            "PLN|Polish Zloty"
        )

        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR", "USD", "WTF")
        viewModel.getCurrentFiat(currencies) shouldBeEqualTo "Euro (EUR)"
        viewModel.getCurrentFiat(currencies) shouldBeEqualTo "US Dollar (USD)"
        viewModel.getCurrentFiat(currencies) shouldBeEqualTo ""
    }

    @Test
    fun `reset tokens should success update live data`(){
        whenever(walletConfigManager.removeAllTokens()).thenReturn(Completable.complete())
        viewModel.resetTokens()
        viewModel.resetTokensLiveData.observeForever(resetTokensObserver)
        resetTokensCaptor.run {
            verify(resetTokensObserver).onChanged(capture())
            firstValue.peekContent().isSuccess shouldBeEqualTo true
        }
    }

    @Test
    fun `reset tokens should fail update live data`(){
        whenever(walletConfigManager.removeAllTokens()).thenReturn(Completable.error(Throwable()))
        viewModel.resetTokens()
        viewModel.resetTokensLiveData.observeForever(resetTokensObserver)
        resetTokensCaptor.run {
            verify(resetTokensObserver).onChanged(capture())
            firstValue.peekContent().isFailure shouldBeEqualTo true
        }
    }
}