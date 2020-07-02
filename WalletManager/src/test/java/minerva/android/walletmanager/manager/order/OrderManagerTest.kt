package minerva.android.walletmanager.manager.order

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.RxTest
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Identity
import minerva.android.walletmanager.model.Service
import minerva.android.walletmanager.model.Value
import minerva.android.walletmanager.model.WalletConfig
import minerva.android.walletmanager.model.defs.WalletActionType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test


class OrderManagerTest : RxTest() {

    private val walletConfigManager: WalletConfigManager = mock()
    private val orderManager = OrderManagerImpl(walletConfigManager)

    private val identities = listOf(
        Identity(1, name = "Identity1"),
        Identity(2, name = "Identity2"),
        Identity(3, name = "Identity2")
    )

    private val values = listOf(
        Value(10, name = "Value1"),
        Value(11, name = "Value2"),
        Value(12, name = "Value3")
    )

    private val services = listOf(
        Service(name = "Service1"),
        Service(name = "Service2"),
        Service(name = "Service3")
    )

    private val walletConfig = WalletConfig(0, identities, values, services)

    @Before
    override fun setupRxSchedulers() {
        super.setupRxSchedulers()
        whenever(walletConfigManager.getWalletConfig()).thenReturn(walletConfig)
    }

    @Test
    fun `Check getting correct account list`() {
        val resultIdentities = orderManager.prepareList(WalletActionType.IDENTITY)
        resultIdentities.size shouldBeEqualTo identities.size
        resultIdentities[0].name shouldBeEqualTo identities[0].name
        val resultValues = orderManager.prepareList(WalletActionType.VALUE)
        resultValues.size shouldBeEqualTo values.size
        resultValues[0].name shouldBeEqualTo values[0].name
        val resultService = orderManager.prepareList(WalletActionType.SERVICE)
        resultService.size shouldBeEqualTo services.size
        resultService[0].name shouldBeEqualTo services[0].name
    }

    @Test
    fun `Saving new order correct`() {
        val newIdentities = listOf(
            Identity(2, name = "Identity2"),
            Identity(1, name = "Identity1"),
            Identity(3, name = "Identity2")
        )
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.complete())

        orderManager.apply {
            val com = updateList(WalletActionType.IDENTITY, newIdentities).test()
            com.isCancelled shouldBeEqualTo false
            com.assertComplete()
        }
    }

    @Test
    fun `Saving new order error`() {
        val error = Throwable("some error")
        val newIdentities = listOf(
            Identity(2, name = "Identity2"),
            Identity(1, name = "Identity1"),
            Identity(3, name = "Identity2")
        )
        whenever(walletConfigManager.updateWalletConfig(any())).thenReturn(Completable.error(error))

        orderManager.apply {
            val com = updateList(WalletActionType.IDENTITY, newIdentities).test()
            com.isCancelled shouldBeEqualTo false
            com.assertError(error)
        }
    }

}