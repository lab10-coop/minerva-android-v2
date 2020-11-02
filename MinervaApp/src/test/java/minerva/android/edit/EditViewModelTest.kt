package minerva.android.edit

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.BaseViewModelTest
import minerva.android.kotlinUtils.event.Event
import minerva.android.observeLiveDataEvent
import minerva.android.observeWithPredicate
import minerva.android.walletmanager.manager.order.OrderManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.defs.WalletActionType
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class EditViewModelTest : BaseViewModelTest() {

    private val orderManager: OrderManager = mock()
    private val viewModel = EditOrderViewModel(orderManager)

    private val saveObserver: Observer<Event<Unit>> = mock()
    private val saveCaptor: KArgumentCaptor<Event<Unit>> = argumentCaptor()

    private val identities = listOf(
        Identity(1, name = "Identity1"),
        Identity(2, name = "Identity2"),
        Identity(3, name = "Identity2")
    )

    private val values = listOf(
        Account(10, name = "Value1"),
        Account(11, name = "Value2"),
        Account(12, name = "Value3")
    )

    private val services = listOf(
        Service(name = "Service1"),
        Service(name = "Service2"),
        Service(name = "Service3")
    )

    @Test
    fun `Get correct list to make order`() {
        whenever(orderManager.prepareList(WalletActionType.IDENTITY)).thenReturn(identities)
        whenever(orderManager.prepareList(WalletActionType.ACCOUNT)).thenReturn(values)
        whenever(orderManager.prepareList(WalletActionType.SERVICE)).thenReturn(services)

        val identitiesResult = viewModel.prepareList(WalletActionType.IDENTITY)
        val valuesResult = viewModel.prepareList(WalletActionType.ACCOUNT)
        val servicesResult = viewModel.prepareList(WalletActionType.SERVICE)

        identitiesResult[0].name shouldBeEqualTo identities[0].name
        valuesResult[0].name shouldBeEqualTo values[0].name
        servicesResult[0].name shouldBeEqualTo services[0].name
    }

    @Test
    fun `Save changes to Wallet Config correctly`() {
        whenever(orderManager.updateList(any(), any())).thenReturn(Completable.complete())
        viewModel.apply {
            saveNewOrderLiveData.observeForever(saveObserver)
            saveChanges(0, listOf())
            saveCaptor.run {
                verify(saveObserver).onChanged(capture())
            }
        }
    }

    @Test
    fun `Save changes to wallet config with error`() {
        val error = Throwable("some error")
        whenever(orderManager.updateList(any(), any())).thenReturn(Completable.error(error))
        viewModel.apply {
            saveChanges(0, listOf())
            errorLiveData.observeLiveDataEvent(Event(error))
        }
    }
}