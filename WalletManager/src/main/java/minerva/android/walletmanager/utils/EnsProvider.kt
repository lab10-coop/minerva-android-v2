package minerva.android.walletmanager.utils

import minerva.android.walletmanager.BuildConfig
import minerva.android.walletmanager.storage.LocalStorage

class EnsProvider(private val localStorage: LocalStorage) {
    val ensUrl: String
        get() =
            if (localStorage.areMainNetsEnabled) BuildConfig.ENS_MAIN_URL
            else BuildConfig.ENS_TEST_URL
}