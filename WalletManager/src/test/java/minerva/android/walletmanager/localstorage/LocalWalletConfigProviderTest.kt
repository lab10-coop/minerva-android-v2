package minerva.android.walletmanager.localstorage

import android.content.SharedPreferences
import io.mockk.*
import minerva.android.configProvider.model.walletConfig.WalletConfigPayload
import minerva.android.walletmanager.walletconfig.localProvider.LocalWalletConfigProviderImpl
import org.junit.Test

class LocalWalletConfigProviderTest {

    private val sharedPref = mockk<SharedPreferences> {
        every { edit().putString(any(), any()).apply() } just Runs
        every { getString(any(), any()) } returns ""
    }

    private val localStorage = LocalWalletConfigProviderImpl(sharedPref)

    @Test
    fun `save wallet config test`(){
        localStorage.saveWalletConfig(WalletConfigPayload())
        verify {
            sharedPref.edit().putString(any(), any()).apply()
        }
        confirmVerified(sharedPref)
    }
}