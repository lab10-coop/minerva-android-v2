package minerva.android.token.ramp

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Single
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.InvalidId
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.token.ERC20Token
import minerva.android.walletmanager.walletActions.WalletActionsRepository
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.model.Network
import okhttp3.internal.notify
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
                Account(1, chainId = 3, name = "account1"),
                Account(2, chainId = 3, name = "account2"),
                Account(3, chainId = 3, name = "account3"),
                Account(4, chainId = 3, name = "account4")
        )
        whenever(accountManager.getAllActiveAccounts(any())).thenReturn(currentAccounts)
        viewModel.apply {
            currentChainId shouldBeEqualTo Int.InvalidId
            getValidAccounts(3)
            currentChainId shouldBeEqualTo 3
            spinnerPosition = 3
            getCurrentAccount().name shouldBeEqualTo "account4"
            spinnerPosition = 0
            getCurrentAccount().name shouldBeEqualTo "account1"
            currentChainId shouldBeEqualTo 3
        }
    }

    //TODO klop test imrovement?
    @Test
    fun `check Creating new Account flow`() {
        val error = Throwable("error")
        NetworkManager.initialize(listOf(Network(chainId = 3, httpRpc = "some_rpc")))
        whenever(accountManager.createRegularAccount(any())).thenReturn(Single.just("Cookie Account"), Single.error(error))
        whenever(walletActionsRepository.saveWalletActions(any())).thenReturn(Completable.complete())
        viewModel.run {
            loadingLiveData.observeForever(loadingObserver)
            createAccountLiveData.observeForever(createObserver)
            errorLiveData.observeForever(errorObserver)
            createNewAccount(3)
            createNewAccount(3)
        }
        createCaptor.run {
            verify(createObserver).onChanged(capture())
        }

        loadingCaptor.run {
            verify(loadingObserver, times(4)).onChanged(capture())
        }
        errorCaptor.run {
            verify(errorObserver).onChanged(capture())
        }
    }
}