package minerva.android.walletmanager.manager.order

import androidx.lifecycle.LiveData
import io.reactivex.Completable
import minerva.android.walletmanager.exception.NotInitializedWalletConfigThrowable
import minerva.android.walletmanager.exception.NotSupportedAccountThrowable
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
                else -> Completable.error(NotSupportedAccountThrowable())
            }
        }
        return Completable.error(NotInitializedWalletConfigThrowable())
    }

    override fun prepareList(type: Int): List<MinervaPrimitive> =
        when (type) {
            WalletActionType.IDENTITY -> prepareIdentitiesList()
            WalletActionType.ACCOUNT -> prepareValuesList()
            WalletActionType.SERVICE -> prepareServicesList()
            else -> listOf()
        }

    override fun isOrderAvailable(type: Int): Boolean {
        walletConfigLiveData.value?.let { config ->
            return when (type) {
                WalletActionType.IDENTITY -> config.identities.filter { !it.isDeleted }.size > ONE_ELEMENT
                WalletActionType.ACCOUNT -> config.accounts.filter { !it.isDeleted }.size > ONE_ELEMENT
                WalletActionType.SERVICE -> config.services.filter { !it.isDeleted }.size > ONE_ELEMENT
                else -> false
            }
        }
        return false
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

    companion object {
        private const val ONE_ELEMENT = 1
    }
}