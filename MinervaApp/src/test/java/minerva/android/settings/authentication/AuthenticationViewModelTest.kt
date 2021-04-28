package minerva.android.settings.authentication

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import minerva.android.walletmanager.storage.LocalStorage
import org.junit.Test

class AuthenticationViewModelTest {

    private val localStorage: LocalStorage = mock()

    private val viewModel = AuthenticationViewModel(localStorage)

    @Test
    fun `Check getting authentication setting` () {
        whenever(localStorage.isProtectKeysEnabled).thenReturn(false)
        viewModel.isProtectKeysEnabled()
        viewModel.toggleProtectKeys()
        verify(localStorage, times(2)).isProtectKeysEnabled
    }
}