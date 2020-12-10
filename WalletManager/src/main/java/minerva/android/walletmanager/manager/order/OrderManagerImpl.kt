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

    override val areMainNetsEnabled
        get() = walletConfigManager.areMainNetworksEnabled

    override fun updateList(type: Int, newOrderList: List<MinervaPrimitive>): Completable {
        getWalletConfig()?.let {
            return when (type) {
                WalletActionType.IDENTITY -> walletConfigManager.updateWalletConfig(
                    it.copy(version = it.updateVersion, identities = (newOrderList as List<Identity>))
                )
                WalletActionType.ACCOUNT -> walletConfigManager.updateWalletConfig(
                    it.copy(version = it.updateVersion, accounts = (newOrderList as List<Account>))
                )
                WalletActionType.SERVICE -> walletConfigManager.updateWalletConfig(
                    it.copy(version = it.updateVersion, services = (newOrderList as List<Service>))
                )
                WalletActionType.CREDENTIAL -> walletConfigManager.updateWalletConfig(
                    it.copy(version = it.updateVersion, credentials = (newOrderList as List<Credential>))
                )
                else -> Completable.error(NotSupportedAccountThrowable())
            }
        }
        throw NotInitializedWalletConfigThrowable()
    }

    override fun prepareList(type: Int): List<MinervaPrimitive> =
        when (type) {
            WalletActionType.IDENTITY -> prepareIdentitiesList()
            WalletActionType.ACCOUNT -> prepareValuesList()
            WalletActionType.SERVICE -> prepareServicesList()
            WalletActionType.CREDENTIAL -> prepareCredentialList()
            else -> listOf()
        }

    override fun isOrderAvailable(type: Int): Boolean {
        walletConfigLiveData.value?.let { config ->
            return when (type) {
                WalletActionType.IDENTITY -> config.identities.filter { !it.isDeleted }.size > ONE_ELEMENT
                WalletActionType.ACCOUNT -> config.accounts.filter { !it.isDeleted && it.network.testNet != areMainNetsEnabled }.size > ONE_ELEMENT
                WalletActionType.SERVICE -> config.services.filter { !it.isDeleted }.size > ONE_ELEMENT
                WalletActionType.CREDENTIAL -> config.credentials.filter { !it.isDeleted }.size > ONE_ELEMENT
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

    private fun prepareCredentialList(): List<MinervaPrimitive> {
        getWalletConfig()?.let { config ->
            return config.credentials
        }
        return listOf()
    }

    companion object {
        private const val ONE_ELEMENT = 1
    }
}