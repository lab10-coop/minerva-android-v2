package minerva.android.settings.fiat

import com.nhaarman.mockitokotlin2.*
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.storage.LocalStorage
import minerva.android.walletmanager.storage.RateStorage
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class FiatViewModelTest {

    private val localStorage: LocalStorage = mock()
    private val accountManager: AccountManager = mock()
    private val rateStorage: RateStorage = mock()
    private val viewModel = FiatViewModel(localStorage, accountManager, rateStorage)

    @Test
    fun `Check getting current fiat position`() {
        whenever(localStorage.loadCurrentFiat()).thenReturn("EUR", "GBP", "USD", "PLN", "WTF")
        with(viewModel) {
            getCurrentFiatPosition() shouldBeEqualTo 0
            getCurrentFiatPosition() shouldBeEqualTo 1
            getCurrentFiatPosition() shouldBeEqualTo 2
            getCurrentFiatPosition() shouldBeEqualTo 33
            getCurrentFiatPosition() shouldBeEqualTo 0
        }

    }

    @Test
    fun `Check that saving new fiat clears correct values`() {
        viewModel.saveCurrentFiat("WTF")
        verify(localStorage, times(1)).saveCurrentFiat(any())
        verify(accountManager, times(1)).clearFiat()
    }
}