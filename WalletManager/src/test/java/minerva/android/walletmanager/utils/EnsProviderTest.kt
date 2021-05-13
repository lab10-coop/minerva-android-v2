package minerva.android.walletmanager.utils

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import minerva.android.walletmanager.storage.LocalStorage
import org.junit.Test

class EnsProviderTest {

    private val localStorage: LocalStorage = mock()
    private val provider = EnsProvider(localStorage)

    @Test
    fun `provider main net url when main networks enabled`() {
        whenever(localStorage.areMainNetworksEnabled).thenReturn(true)
        val result = provider.ensUrl
        result.contains("mainnet")
    }

    @Test
    fun `provider test net url when main networks disabled`() {
        whenever(localStorage.areMainNetworksEnabled).thenReturn(false)
        val result = provider.ensUrl
        result.contains("infura")
    }
}