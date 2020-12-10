package minerva.android.settings

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.repository.seed.MasterSeedRepository

class SettingsViewModel(private val masterSeedRepository: MasterSeedRepository) : ViewModel() {

    fun areMainNetworksEnabled(isChecked: Boolean) {
        masterSeedRepository.toggleMainNetsEnabled = isChecked
    }

    val areMainNetsEnabled: Boolean
        get() = masterSeedRepository.areMainNetworksEnabled

    val isMnemonicRemembered
        get() = masterSeedRepository.isMnemonicRemembered()

    val isSynced
        get() = masterSeedRepository.isSynced
}