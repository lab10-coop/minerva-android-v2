package minerva.android.manage

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.manager.accounts.AccountManager
import minerva.android.walletmanager.model.Account
import minerva.android.walletmanager.model.Asset
import minerva.android.walletmanager.model.AssetVisibilitySettings
import minerva.android.walletmanager.storage.LocalStorage

class ManageAssetsViewModel(
    private val accountManager: AccountManager,
    private val localStorage: LocalStorage
) : BaseViewModel() {

    lateinit var account: Account
    private lateinit var assetVisibilitySettings: AssetVisibilitySettings

    fun initViewModel(index: Int) {
        account = accountManager.loadAccount(index)
        assetVisibilitySettings = localStorage.getAssetVisibilitySettings()
    }

    fun loadAssets() = account.network.let {
        listOf(Asset(it.full, it.short)) + account.network.assets
    }

    fun getAssetVisibilitySettings(assetAddress: String): Boolean =
        assetVisibilitySettings.getAssetVisibility(account.address, assetAddress) ?: false


    fun saveAssetVisibilitySettings(assetAddress: String, visibility: Boolean) {
        assetVisibilitySettings = localStorage.saveAssetVisibilitySettings(
            assetVisibilitySettings.updateAssetVisibility(account.address, assetAddress, visibility)
        )
    }
}