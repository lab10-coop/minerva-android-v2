package minerva.android.backup

import com.nhaarman.mockitokotlin2.mock
import minerva.android.settings.backup.BackupViewModel
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import org.junit.Test
import kotlin.test.assertEquals

class BackupViewModelTest {

    private val masterSeedRepository: MasterSeedRepository = mock()
    private val viewModel = BackupViewModel(masterSeedRepository)

    @Test
    fun `test formatting mnemonic`() {
        val mnemonic = viewModel.getFormattedMnemonic("asd asd asd asd asd")
        assertEquals("asd\nasd\nasd\nasd\nasd\n", mnemonic)
    }
}