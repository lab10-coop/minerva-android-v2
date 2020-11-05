package minerva.android.settings

import androidx.lifecycle.ViewModel
import minerva.android.walletmanager.repository.seed.MasterSeedRepository

class SettingsViewModel(private val masterSeedRepository: MasterSeedRepository) : ViewModel() {
    val isMnemonicRemembered
        get() = masterSeedRepository.isMnemonicRemembered()

    val isSynced
        get() = masterSeedRepository.isSynced
}