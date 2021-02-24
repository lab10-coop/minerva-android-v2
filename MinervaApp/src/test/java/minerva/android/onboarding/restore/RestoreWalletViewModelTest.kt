package minerva.android.onboarding.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestoreWalletViewModelTest : BaseViewModelTest() {

    private val masterSeedRepository: MasterSeedRepository = mock()
    private val viewModel = RestoreWalletViewModel(masterSeedRepository)

    private val invalidMnemonicObserver: Observer<Event<List<String>>> = mock()
    private val invalidMnemonicCaptor: KArgumentCaptor<Event<List<String>>> = argumentCaptor()

    private val walletConfigNotFoundObserver: Observer<Event<Unit>> = mock()
    private val walletConfigNotFoundCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    @Before
    fun setup() {
        val liveData: LiveData<WalletConfig> = MutableLiveData<WalletConfig>()
        whenever(masterSeedRepository.walletConfigLiveData) doReturn liveData
    }

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
    fun `test restore wallet from mnemonic file not found error`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve picture"
        val error = Throwable()
        whenever(masterSeedRepository.restoreMasterSeed(any())).doReturn(Completable.error(error))
        whenever(masterSeedRepository.validateMnemonic(mnemonic)).thenReturn(emptyList())
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
        whenever(masterSeedRepository.restoreMasterSeed(any())).doReturn(Completable.error(error))
        viewModel.validateMnemonic(mnemonic)
        viewModel.walletConfigNotFoundLiveData.observeLiveDataEvent(Event(Unit))
    }

    @Test
    fun `test restore wallet from invalid mnemonic `() {
        val mnemonic = "vessel ladder alter error federal hoho chat ability sun glass valve hehe"
        whenever(masterSeedRepository.validateMnemonic(any())).thenReturn(listOf("hoho, hehe"))
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
        whenever(masterSeedRepository.validateMnemonic(any())).thenReturn(listOf("hoho, hehe"))
        viewModel.invalidMnemonicLiveData.observeForever(invalidMnemonicObserver)
        viewModel.validateMnemonic(mnemonic)
        invalidMnemonicCaptor.run {
            verify(invalidMnemonicObserver).onChanged(capture())
            firstValue.peekContent().isEmpty()
        }
    }
}