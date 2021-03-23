package minerva.android.walletmanager.manager.order

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.manager.networks.NetworkManager
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.Network
import minerva.android.walletmanager.model.defs.WalletActionType
import minerva.android.walletmanager.model.minervaprimitives.Identity
import minerva.android.walletmanager.model.minervaprimitives.Service
import minerva.android.walletmanager.model.minervaprimitives.account.Account
import minerva.android.walletmanager.model.wallet.WalletConfig
import minerva.android.walletmanager.utils.RxTest
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
        Account(10, name = "Value1", chainId = 1),
        Account(11, name = "Value2", chainId = 2),
        Account(12, name = "Value3", chainId = 3)
    )

    private val services = listOf(
        Service(name = "Service1")
    )

    private val networks = listOf(
        Network(chainId = 1, httpRpc = "some"),
        Network(chainId = 2, httpRpc = "some"),
        Network(chainId = 3, httpRpc = "some")
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
        val resultValues = orderManager.prepareList(WalletActionType.ACCOUNT)
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

    @Test
    fun `Is edit order icon shown correct`() {
        NetworkManager.initialize(networks)
        walletConfig.accounts[1].isDeleted = true
        walletConfig.accounts[2].isDeleted = true
        val walletConfigLD = MutableLiveData<Event<WalletConfig>>()
        walletConfigLD.value = Event(walletConfig)
        whenever(walletConfigManager.walletConfigLiveData).thenReturn(walletConfigLD)
        val isIdentityIconShown = orderManager.isOrderAvailable(WalletActionType.IDENTITY)
        isIdentityIconShown shouldBeEqualTo true
        val isAccountIconShown = orderManager.isOrderAvailable(WalletActionType.ACCOUNT)
        isAccountIconShown shouldBeEqualTo false
        val isServiceIconShown = orderManager.isOrderAvailable(WalletActionType.SERVICE)
        isServiceIconShown shouldBeEqualTo false
    }
}