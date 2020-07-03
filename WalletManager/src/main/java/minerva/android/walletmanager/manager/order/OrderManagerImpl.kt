package minerva.android.walletmanager.manager.order

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.walletmanager.manager.wallet.WalletConfigManager
import minerva.android.walletmanager.model.*
import minerva.android.walletmanager.model.defs.WalletActionType

class OrderManagerImpl(private val walletConfigManager: WalletConfigManager) : OrderManager {

    override val walletConfigLiveData: LiveData<WalletConfig>
        get() = walletConfigManager.walletConfigLiveData

    override fun updateList(type: Int, newOrderList: List<MinervaPrimitive>): Completable {
        getWalletConfig()?.let {
            return when (type) {
                WalletActionType.IDENTITY -> walletConfigManager.updateWalletConfig(
                    WalletConfig(it.updateVersion, (newOrderList as List<Identity>), it.accounts, it.services)
                )
                WalletActionType.ACCOUNT -> walletConfigManager.updateWalletConfig(
                    WalletConfig(it.updateVersion, it.identities, (newOrderList as List<Account>), it.services)
                )
                WalletActionType.SERVICE -> walletConfigManager.updateWalletConfig(
                    WalletConfig(it.updateVersion, it.identities, it.accounts, (newOrderList as List<Service>))
                )
                else -> Completable.error(Throwable("Not supported Account type"))
            }
        }
        return Completable.error(Throwable("Wallet Config was not initialized"))
    }

    override fun prepareList(type: Int): List<MinervaPrimitive> =
        when (type) {
            WalletActionType.IDENTITY -> prepareIdentitiesList()
            WalletActionType.ACCOUNT -> prepareValuesList()
            WalletActionType.SERVICE -> prepareServicesList()
            else -> listOf()
        }

    private fun getWalletConfig() = walletConfigManager.getWalletConfig()

    private fun prepareIdentitiesList(): List<MinervaPrimitive> {
        getWalletConfig()?.let {
            return it.identities
        }
        return listOf()
    }

    private fun prepareValuesList(): List<MinervaPrimitive> {
        getWalletConfig()?.let {
            return it.accounts
        }
        return listOf()
    }

    private fun prepareServicesList(): List<MinervaPrimitive> {
        getWalletConfig()?.let {
            return it.services
        }
        return listOf()
    }
}