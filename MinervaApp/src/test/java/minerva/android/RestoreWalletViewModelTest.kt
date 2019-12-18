package minerva.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import minerva.android.onboarding.restore.RestoreWalletViewModel
import minerva.android.walletmanager.manager.WalletManager
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestoreWalletViewModelTest {

    private val walletManager: WalletManager = mock()
    private val viewModel = RestoreWalletViewModel(walletManager)

    private val validateMnemonicObserver: Observer<List<String>> = mock()
    private val validateMnemonicCaptor: KArgumentCaptor<List<String>> = argumentCaptor()

    @get:Rule
    val rule
        get() = InstantTaskExecutorRule()

    @Test
    fun `check mnemonic length validation`() {
        assertTrue { viewModel.isMnemonicLengthValid("sfds fd fds d ds asd asd das das sda asd da") }
        assertTrue { viewModel.isMnemonicLengthValid("sfds % fds d ds &asd        das das , asd da *") }
        assertFalse { viewModel.isMnemonicLengthValid("sfds fd fds d ds asd asd") }
        assertFalse { viewModel.isMnemonicLengthValid("sfds fd fds d ds asd asd             ") }
        assertFalse { viewModel.isMnemonicLengthValid("") }
        assertFalse { viewModel.isMnemonicLengthValid(" ") }
        assertTrue { viewModel.isMnemonicLengthValid("4 3 6 434 7 8 65 # $ % o o") }
    }

    @Test
    fun `test mnemonic validator`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        viewModel.validateMnemonicLiveData.observeForever(validateMnemonicObserver)
        viewModel.validateMnemonic(mnemonic)
        validateMnemonicCaptor.run {
            verify(validateMnemonicObserver).onChanged(capture())
            firstValue.isEmpty()
        }
    }

    @Test
    fun `test mnemonic invalid words collection`() {
        val mnemonic = "vessel ladder alter && federal wrongWord chat ability sun *( valve picture"
        whenever(walletManager.validateMnemonic(any())).thenReturn(listOf("&&, wrongWord"))
        viewModel.validateMnemonicLiveData.observeForever(validateMnemonicObserver)
        viewModel.validateMnemonic(mnemonic)
        validateMnemonicCaptor.run {
            verify(validateMnemonicObserver).onChanged(capture())
            firstValue[0].isNotEmpty() && firstValue == listOf("&&, wrongWord *(")
        }
    }

    @Test
    fun `test validator when mnemonic is empty`() {
        val mnemonic = ""
        whenever(walletManager.validateMnemonic(any())).thenReturn(emptyList())
        viewModel.validateMnemonicLiveData.observeForever(validateMnemonicObserver)
        viewModel.validateMnemonic(mnemonic)
        validateMnemonicCaptor.run {
            verify(validateMnemonicObserver).onChanged(capture())
            firstValue.isEmpty()
        }
    }
}