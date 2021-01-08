package minerva.android.walletmanager.utils

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import minerva.android.walletmanager.storage.LocalStorage
import org.junit.Test
import kotlin.test.assertEquals

class EnsProviderTest {

    private val localStorage: LocalStorage = mock()
    private val provider = EnsProvider(localStorage)

    @Test
    fun `provider main net url when main networks enabled`() {
        whenever(localStorage.areMainNetsEnabled).thenReturn(true)
        val result = provider.ensUrl
        assertEquals(result, "https://mainnet.infura.io/v3/c7ec643b8c764cb5930bca18fb763469")
    }

    @Test
    fun `provider test net url when main networks disabled`() {
        whenever(localStorage.areMainNetsEnabled).thenReturn(false)
        val result = provider.ensUrl
        assertEquals(result, "https://ropsten.infura.io/v3/c7ec643b8c764cb5930bca18fb763469")
    }
}