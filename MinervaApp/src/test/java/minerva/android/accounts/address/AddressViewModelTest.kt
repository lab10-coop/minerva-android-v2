package minerva.android.accounts.address

import androidx.lifecycle.Observer
import minerva.android.BaseViewModelTest
import minerva.android.walletmanager.manager.identity.IdentityManager
import com.nhaarman.mockitokotlin2.*
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.MinervaPrimitive
import minerva.android.wrapped.WrappedFragmentType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AddressViewModelTest : BaseViewModelTest() {

    private val identityManager: IdentityManager = mock()
    private val accountManager: AccountManager = mock()

    private val viewModel: AddressViewModel = AddressViewModel(identityManager, accountManager)

    private val loadObserver: Observer<Event<MinervaPrimitive>> = mock()
    private val loadCaptor: KArgumentCaptor<Event<MinervaPrimitive>> = argumentCaptor()

    @Test
    fun `Check that load delivers correct object` () {
        val identity = Identity(0, name = "identity1")
        whenever(identityManager.loadIdentity(any(), any())).thenReturn(identity)
        viewModel.loadMinervaPrimitiveLiveData.observeForever(loadObserver)
        viewModel.loadMinervaPrimitive(WrappedFragmentType.IDENTITY_ADDRESS, 0)
        loadCaptor.run {
            verify(loadObserver).onChanged(capture())
            (firstValue.peekContent() as Identity).name shouldBeEqualTo "identity1"
        }
    }

    @Test
    fun `Check that viewModel load correct account` () {
        val account1 = Account(2, name = "account2")
        whenever(accountManager.loadAccount(any())).thenReturn(account1)
        viewModel.loadMinervaPrimitiveLiveData.observeForever(loadObserver)
        viewModel.loadMinervaPrimitive(WrappedFragmentType.ACCOUNT_ADDRESS, 2)
        loadCaptor.run {
            verify(loadObserver).onChanged(capture())
            (firstValue.peekContent() as Account).name shouldBeEqualTo "account2"
        }
    }
}