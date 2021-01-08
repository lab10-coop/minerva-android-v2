package minerva.android.settings

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import minerva.android.BaseViewModelTest
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import org.junit.Test
import kotlin.test.assertEquals

class SettingsViewModelTest : BaseViewModelTest() {

    private val masterSeedRepository: MasterSeedRepository = mock()
    private val viewModel = SettingsViewModel(masterSeedRepository)

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
}