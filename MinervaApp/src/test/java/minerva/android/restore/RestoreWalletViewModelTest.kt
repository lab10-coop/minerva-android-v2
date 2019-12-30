package minerva.android.restore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Single
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.observeWithPredicate
import minerva.android.onboarding.restore.RestoreWalletViewModel
import minerva.android.walletmanager.manager.WalletManager
import minerva.android.walletmanager.model.RestoreWalletResponse
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqualTo
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestoreWalletViewModelTest {

    private val walletManager: WalletManager = mock()
    private val viewModel = RestoreWalletViewModel(walletManager)

    private val invalidMnemonicObserver: Observer<Event<List<String>>> = mock()
    private val invalidMnemonicCaptor: KArgumentCaptor<Event<List<String>>> = argumentCaptor()

    private val walletConfigNotFoundObserver: Observer<Event<Unit>> = mock()
    private val walletConfigNotFoundCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val restoreWalletObserver: Observer<Event<RestoreWalletResponse>> = mock()
    private val restoreWalletCaptor: KArgumentCaptor<Event<RestoreWalletResponse>> = argumentCaptor()

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
    fun `test restore wallet from mnemonic success`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        whenever(walletManager.getWalletConfig(any())).thenReturn(Single.just(RestoreWalletResponse("success", "File fetched")))
        viewModel.invalidMnemonicLiveData.observeForever(invalidMnemonicObserver)
        viewModel.restoreWalletLiveData.observeForever(restoreWalletObserver)
        viewModel.validateMnemonic(mnemonic)
        walletManager.restoreMasterKey(mnemonic) { _, _, _ ->
            restoreWalletCaptor.run {
                verify(restoreWalletObserver).onChanged(capture())
                this.firstValue.peekContent().state shouldBe "success"
            }
        }
    }

    @Test
    fun `test restore wallet from mnemonic file not found error`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        whenever(walletManager.getWalletConfig(any())).thenReturn(Single.just(RestoreWalletResponse("error", "File fetched error")))
        viewModel.invalidMnemonicLiveData.observeForever(invalidMnemonicObserver)
        viewModel.walletConfigNotFoundLiveData.observeForever(walletConfigNotFoundObserver)
        viewModel.validateMnemonic(mnemonic)
        walletManager.restoreMasterKey(mnemonic) { _, _, _ ->
            restoreWalletCaptor.run {
                this.firstValue.peekContent().state shouldBe "error"
            }
            walletConfigNotFoundCaptor.run {
                verify(walletConfigNotFoundObserver).onChanged(capture())
            }
        }
    }

    @Test
    fun `test restore wallet from mnemonic error`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        walletManager.restoreMasterKey(mnemonic) { _, _, _ ->
            whenever(walletManager.getWalletConfig(any())).thenReturn(Single.error(Throwable()))
            viewModel.validateMnemonic(mnemonic)
            viewModel.errorLiveData.observeLiveDataEvent(Event(Throwable()))
        }
    }

    @Test
    fun `test restore wallet from invalid mnemonic `() {
        val mnemonic = "vessel ladder alter error federal hoho chat ability sun glass valve hehe"
        whenever(walletManager.validateMnemonic(any())).thenReturn(listOf("hoho, hehe"))
        viewModel.invalidMnemonicLiveData.observeForever(invalidMnemonicObserver)
        viewModel.validateMnemonic(mnemonic)
        invalidMnemonicCaptor.run {
            verify(invalidMnemonicObserver).onChanged(capture())
            firstValue.peekContent().isNotEmpty() &&
                    firstValue.peekContent() == listOf("hoho, hehe")
        }
    }

    @Test
    fun `test restore wallet from empty mnemonic `() {
        val mnemonic = ""
        whenever(walletManager.validateMnemonic(any())).thenReturn(listOf("hoho, hehe"))
        viewModel.invalidMnemonicLiveData.observeForever(invalidMnemonicObserver)
        viewModel.validateMnemonic(mnemonic)
        invalidMnemonicCaptor.run {
            verify(invalidMnemonicObserver).onChanged(capture())
            firstValue.peekContent().isEmpty()
        }
    }
}