package minerva.android.onboarding.restore

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.manager.wallet.WalletManager
import minerva.android.walletmanager.model.MasterSeed
import minerva.android.walletmanager.model.RestoreWalletResponse
import org.amshove.kluent.shouldBe
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestoreWalletViewModelTest: BaseViewModelTest() {

    private val walletManager: WalletManager = mock()
    private val viewModel = RestoreWalletViewModel(walletManager)

    private val invalidMnemonicObserver: Observer<Event<List<String>>> = mock()
    private val invalidMnemonicCaptor: KArgumentCaptor<Event<List<String>>> = argumentCaptor()

    private val walletConfigNotFoundObserver: Observer<Event<Unit>> = mock()
    private val walletConfigNotFoundCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val restoreWalletObserver: Observer<Event<RestoreWalletResponse>> = mock()
    private val restoreWalletCaptor: KArgumentCaptor<Event<RestoreWalletResponse>> = argumentCaptor()

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
        whenever(walletManager.restoreMasterSeed(any())).doReturn(Single.just(MasterSeed("1", "12", "123")))
        whenever(walletManager.getWalletConfig(any())).thenReturn(Single.just(RestoreWalletResponse("success", "File fetched")))
        whenever( walletManager.validateMnemonic(mnemonic)).thenReturn(emptyList())
        viewModel.invalidMnemonicLiveData.observeForever(invalidMnemonicObserver)
        viewModel.restoreWalletLiveData.observeForever(restoreWalletObserver)
        viewModel.validateMnemonic(mnemonic)
        restoreWalletCaptor.run {
            verify(restoreWalletObserver).onChanged(capture())
            this.firstValue.peekContent().state shouldBe "success"
        }
    }

    @Test
    fun `test restore wallet from mnemonic file not found error`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        whenever(walletManager.restoreMasterSeed(any())).doReturn(Single.just(MasterSeed()))
        whenever(walletManager.getWalletConfig(any())).thenReturn(Single.just(RestoreWalletResponse("error", "File fetched error")))
        whenever( walletManager.validateMnemonic(mnemonic)).thenReturn(emptyList())
        viewModel.invalidMnemonicLiveData.observeForever(invalidMnemonicObserver)
        viewModel.walletConfigNotFoundLiveData.observeForever(walletConfigNotFoundObserver)
        viewModel.validateMnemonic(mnemonic)
        walletConfigNotFoundCaptor.run {
            verify(walletConfigNotFoundObserver).onChanged(capture())
        }
    }

    @Test
    fun `test restore wallet from mnemonic error`() {
        val error = Throwable()
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        whenever(walletManager.restoreMasterSeed(any())).doReturn(Single.error(error))
        whenever(walletManager.getWalletConfig(any())).thenReturn(Single.error(error))
        viewModel.validateMnemonic(mnemonic)
        viewModel.errorLiveData.observeLiveDataEvent(Event(error))
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