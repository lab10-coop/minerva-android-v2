package minerva.android.walletmanager.localstorage

import android.content.SharedPreferences
import io.mockk.*
import minerva.android.configProvider.model.walletActions.WalletActionsConfigPayload
import minerva.android.walletmanager.walletActions.localProvider.LocalWalletActionsConfigProviderImpl
import org.junit.Test

class LocalWalletActionsConfigProviderTest {

    private val sharedPref = mockk<SharedPreferences> {
        every { edit().putString(any(), any()).apply() } just Runs
        every { getString(any(), any()) } returns ""
    }

    private val localWalletActionsConfigProvider = LocalWalletActionsConfigProviderImpl(sharedPref)

    @Test
    fun `save wallet action config test`() {
        localWalletActionsConfigProvider.saveWalletActionsConfig(WalletActionsConfigPayload())
        verify {
            sharedPref.edit().putString(any(), any()).apply()
        }
        confirmVerified(sharedPref)
    }

    @Test
    fun `load wallet action config test`() {
        localWalletActionsConfigProvider.loadWalletActionsConfig()
        verify {
            sharedPref.getString(any(), any())
        }
        confirmVerified(sharedPref)
    }
}