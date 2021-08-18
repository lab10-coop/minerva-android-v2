package minerva.android.onboarding.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.onboarding.restore.state.*
import minerva.android.walletmanager.exception.WalletConfigNotFoundThrowable
import minerva.android.walletmanager.model.wallet.MasterSeed
import minerva.android.walletmanager.model.wallet.MasterSeedError
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import org.junit.Before
import org.junit.Test

class RestoreWalletViewModelTest : BaseViewModelTest() {

    private val masterSeedRepository: MasterSeedRepository = mock()
    private val viewModel = RestoreWalletViewModel(masterSeedRepository)

    private val restoreWalletObserver: Observer<RestoreWalletState> = mock()
    private val restoreWalletStateCaptor: KArgumentCaptor<RestoreWalletState> = argumentCaptor()

    @Before
    fun setup() {
        val liveData: LiveData<Event<WalletConfig>> = MutableLiveData<Event<WalletConfig>>()
        whenever(masterSeedRepository.walletConfigLiveData) doReturn liveData
    }

    @Test
    fun `mnemonic should have min 12 words test`() {
        val mnemonic = "vessel ladder alter error federal sibling chat ability sun glass valve" //11 words
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.validateMnemonic(mnemonic)
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver).onChanged(capture())
            firstValue == InvalidMnemonicLength
        }
    }

    @Test
    fun `mnemonic should have max 24 words test`() {
        val mnemonic =
            "vessel ladder alter error federal sibling chat ability sun glass valve cat vessel ladder alter error federal sibling chat ability sun glass valve cat dog" //25 words
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.validateMnemonic(mnemonic)
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver).onChanged(capture())
            firstValue == InvalidMnemonicLength
        }
    }

    @Test
    fun `mnemonic is not divisible by 3 test`() {
        val mnemonic =
            "vessel ladder alter error federal sibling chat ability sun glass valve hot wear false dog cat" //16 words
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.validateMnemonic(mnemonic)
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver).onChanged(capture())
            firstValue == InvalidMnemonicLength
        }
    }

    @Test
    fun `mnemonic is divisible by 3 and have correct length but incorrect words test`() {
        val mnemonic =
            "vessel ladder alter error federal sibling chat ability sun glass valve hot wear false guedsad" //15 words
        whenever(masterSeedRepository.areMnemonicWordsValid(any())).thenReturn(false)
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.validateMnemonic(mnemonic)
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver).onChanged(capture())
            firstValue == InvalidMnemonicWords
        }
    }

    @Test
    fun `mnemonic is divisible by 3, have correct length and correct words but cannot create seed test`() {
        val mnemonic =
            "vessel ladder alter error federal sibling chat ability sun glass valve hot wear false cat" //15 words
        val error = Throwable("Cannot create seed")
        whenever(masterSeedRepository.areMnemonicWordsValid(any())).thenReturn(true)
        whenever(masterSeedRepository.restoreMasterSeed(any())).thenReturn(MasterSeedError(error))
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.validateMnemonic(mnemonic)
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver).onChanged(capture())
            firstValue == InvalidMnemonicWords
        }
    }

    @Test
    fun `mnemonic is divisible by 3, have correct length, correct words and can create seed test`() {
        val mnemonic =
            "vessel ladder alter error federal sibling chat ability sun glass valve hot wear false cat" //15 words
        val masterSeed = MasterSeed("seed", "publicK", "privateK")
        whenever(masterSeedRepository.areMnemonicWordsValid(any())).thenReturn(true)
        whenever(masterSeedRepository.restoreMasterSeed(any())).thenReturn(masterSeed)
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.validateMnemonic(mnemonic)
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver).onChanged(capture())
            firstValue == ValidMnemonic
        }
    }

    @Test
    fun `test restore wallet success`() {
        whenever(masterSeedRepository.restoreWalletConfig(any())).doReturn(Completable.complete())
        viewModel.masterSeed = MasterSeed("seed", "publicK", "privateK")
        viewModel.restoreWallet()
        verify(masterSeedRepository).initWalletConfig()
        verify(masterSeedRepository).saveIsMnemonicRemembered()
    }

    @Test
    fun `restore wallet and backup file is not found test`() {
        val error = WalletConfigNotFoundThrowable()
        whenever(masterSeedRepository.restoreWalletConfig(any())).doReturn(Completable.error(error))
        viewModel.masterSeed = MasterSeed("seed", "publicK", "privateK")
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.restoreWallet()
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver, times(3)).onChanged(capture())
            firstValue == WalletConfigNotFound
        }
    }

    @Test
    fun `restore wallet and server error occurs test`() {
        val error = Throwable()
        whenever(masterSeedRepository.restoreWalletConfig(any())).doReturn(Completable.error(error))
        viewModel.masterSeed = MasterSeed("seed", "publicK", "privateK")
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.restoreWallet()
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver, times(3)).onChanged(capture())
            firstValue == GenericServerError
        }
    }

    @Test
    fun `create wallet new backup file`() {
        whenever(masterSeedRepository.createWalletConfig(any())).doReturn(Completable.complete())
        viewModel.restoreWalletState.observeForever(restoreWalletObserver)
        viewModel.masterSeed = MasterSeed("seed", "publicK", "privateK")
        viewModel.createWalletConfig()
        restoreWalletStateCaptor.run {
            verify(restoreWalletObserver, times(3)).onChanged(capture())
            firstValue == WalletConfigCreated
        }
    }
}