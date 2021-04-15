package minerva.android.walletmanager.utils

import minerva.android.walletmanager.BuildConfig
import minerva.android.walletmanager.storage.LocalStorage

class EnsProvider(private val localStorage: LocalStorage) {
    val ensUrl: String
        get() {
            val url = if (localStorage.areMainNetworksEnabled) BuildConfig.ENS_MAIN_URL
            else BuildConfig.ENS_TEST_URL
            return "$url${BuildConfig.INFURA_API_KEY}"
        }
}