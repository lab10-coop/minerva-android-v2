package minerva.android.settings

import androidx.lifecycle.ViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.function.orElse
import minerva.android.kotlinUtils.mapper.StringArrayMapper
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import minerva.android.walletmanager.storage.LocalStorage

class SettingsViewModel(private val masterSeedRepository: MasterSeedRepository, private val localStorage: LocalStorage) :
    ViewModel() {

    fun areMainNetworksEnabled(isChecked: Boolean) {
        masterSeedRepository.areMainNetworksEnabled = isChecked
    }

    val areMainNetsEnabled: Boolean
        get() = masterSeedRepository.areMainNetworksEnabled

    val isMnemonicRemembered
        get() = masterSeedRepository.isMnemonicRemembered()

    val isSynced
        get() = masterSeedRepository.isSynced

    val isAuthenticationEnabled
        get() = localStorage.isProtectKeysEnabled

    fun getCurrentFiat(currencies: Array<String>): String = localStorage.loadCurrentFiat().let { fiat ->
        StringArrayMapper.mapStringArray(currencies)[fiat]?.let {
            String.format(FIAT_HEADER_FORMAT, it, fiat)
        }.orElse { String.Empty }
    }

    companion object {
        private const val FIAT_HEADER_FORMAT = "%s (%s)"
    }
}