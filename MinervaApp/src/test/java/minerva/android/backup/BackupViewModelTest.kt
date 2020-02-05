package minerva.android.backup

import com.nhaarman.mockitokotlin2.mock
import minerva.android.settings.backup.BackupViewModel
import minerva.android.walletmanager.manager.WalletManager
import org.junit.Test
import kotlin.test.assertEquals

class BackupViewModelTest {

    private val walletManager: WalletManager = mock()
    private val viewModel = BackupViewModel(walletManager)

    @Test
    fun`test formatting mnemonic`(){
        val mnemonic = viewModel.getFormattedMnemonic("asd asd asd asd asd")
        assertEquals("asd\nasd\nasd\nasd\nasd\n", mnemonic)
    }
}