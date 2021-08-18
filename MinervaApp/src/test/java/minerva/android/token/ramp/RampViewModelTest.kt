package minerva.android.token.ramp

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class RampViewModelTest : BaseViewModelTest() {

    private val walletActionsRepository: WalletActionsRepository = mock()
    private val accountManager: AccountManager = mock()
    private val viewModel: RampViewModel = RampViewModel(walletActionsRepository, accountManager)

    private val loadingObserver: Observer<Event<Boolean>> = mock()
    private val loadingCaptor: KArgumentCaptor<Event<Boolean>> = argumentCaptor()

    private val createObserver: Observer<Event<Unit>> = mock()
    private val createCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val errorObserver: Observer<Event<Throwable>> = mock()
    private val errorCaptor: KArgumentCaptor<Event<Throwable>> = argumentCaptor()

    @Test
    fun `Check getting active account by chainId and getting currently chosen Account`() {
        val currentAccounts = listOf(
            Account(1, chainId = 3, name = "account1", address = "0x0"),
            Account(2, chainId = 3, name = "account2", address = "0x0"),
            Account(3, chainId = 3, name = "account3", address = "0x0"),
            Account(4, chainId = 3, name = "account4", address = "0x0")
        )
        whenever(accountManager.getAllActiveAccounts(any())).thenReturn(currentAccounts)
        whenever(accountManager.toChecksumAddress(any())).thenReturn("0xCHECKxSUM")
        viewModel.apply {
            currentChainId shouldBeEqualTo Int.InvalidId
            currentChainId = 3
            getValidAccounts(3)
            currentChainId shouldBeEqualTo 3
            spinnerPosition = 3
            getCurrentCheckSumAddress() shouldBeEqualTo "0xCHECKxSUM"
            spinnerPosition = 0
            getCurrentCheckSumAddress() shouldBeEqualTo "0xCHECKxSUM"
            currentChainId shouldBeEqualTo 3
        }
    }

    @Test
    fun `Check getting active account by chainId with limitation`() {
        val currentAccounts = listOf(
            Account(1, chainId = 3, name = "account1", address = "0x0"),
            Account(2, chainId = 3, name = "account2", address = "0x0"),
            Account(3, chainId = 3, name = "account3", address = "0x0"),
            Account(4, chainId = 3, name = "account4", address = "0x0")
        )
        whenever(accountManager.getAllActiveAccounts(any())).thenReturn(currentAccounts)
        whenever(accountManager.getNumberOfAccountsToUse()).thenReturn(currentAccounts.size)
        val accountsWithLimitation = viewModel.getValidAccountsAndLimit(chainId = 3)
        accountsWithLimitation.first shouldBeEqualTo 4
        accountsWithLimitation.second shouldBeEqualTo currentAccounts
    }


    @Test
    fun `check Creating new Account flow`() {
        val error = Throwable("error")
        NetworkManager.initialize(listOf(Network(chainId = 3, httpRpc = "some_rpc")))
        whenever(accountManager.createOrUnhideAccount(any())).thenReturn(Single.just("Cookie Account"), Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.run {
            loadingLiveData.observeForever(loadingObserver)
            createAccountLiveData.observeForever(createObserver)
            errorLiveData.observeForever(errorObserver)
            currentChainId = 3
            createNewAccount()
            createNewAccount()
        }

        verify(createObserver).onChanged(createCaptor.capture())
        verify(loadingObserver, times(4)).onChanged(loadingCaptor.capture())
        verify(errorObserver).onChanged(errorCaptor.capture())
    }
}