package minerva.android.walletmanager.manager.order

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionType

class OrderManagerImpl(private val walletConfigManager: WalletConfigManager) : OrderManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun updateList(type: Int, newOrderList: List<Account>): Completable {
        getWalletConfig()?.let {
            return when (type) {
                WalletActionType.IDENTITY -> walletConfigManager.updateWalletConfig(
                    WalletConfig(it.updateVersion, (newOrderList as List<Identity>), it.values, it.services)
                )
                WalletActionType.VALUE -> walletConfigManager.updateWalletConfig(
                    WalletConfig(it.updateVersion, it.identities, (newOrderList as List<Value>), it.services)
                )
                WalletActionType.SERVICE -> walletConfigManager.updateWalletConfig(
                    WalletConfig(it.updateVersion, it.identities, it.values, (newOrderList as List<Service>))
                )
                else -> Completable.error(Throwable("Not supported Account type"))
            }
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    override fun prepareList(type: Int): List<Account> =
        when (type) {
            WalletActionType.IDENTITY -> prepareIdentitiesList()
            WalletActionType.VALUE -> prepareValuesList()
            WalletActionType.SERVICE -> prepareServicesList()
            else -> listOf()
        }

    private fun getWalletConfig() = walletConfigManager.getWalletConfig()

    private fun prepareIdentitiesList(): List<Account> {
        getWalletConfig()?.let {
            return it.identities
        }
        return listOf()
    }

    private fun prepareValuesList(): List<Account> {
        getWalletConfig()?.let {
            return it.values
        }
        return listOf()
    }

    private fun prepareServicesList(): List<Account> {
        getWalletConfig()?.let {
            return it.services
        }
        return listOf()
    }
}