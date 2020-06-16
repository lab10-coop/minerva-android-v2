package minerva.android.settings.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.event.Event
import minerva.android.walletmanager.repository.seed.MasterSeedRepository
import java.util.*

class BackupViewModel(private val masterSeedRepository: MasterSeedRepository) : ViewModel() {

    var mnemonic: String = String.Empty

    private val _showMnemonicMutableLiveData = MutableLiveData<Event<String>>()
    val showMnemonicLiveData: LiveData<Event<String>> get() = _showMnemonicMutableLiveData

    fun showMnemonic() {
        masterSeedRepository.getMnemonic().apply {
            mnemonic = this
            _showMnemonicMutableLiveData.value = Event(getFormattedMnemonic(mnemonic))
        }
    }

    fun getFormattedMnemonic(mnemonic: String): String {
        val phase = StringTokenizer(mnemonic)
        var words: String = String.Empty
        while (phase.hasMoreTokens()) {
            words += "${phase.nextToken()}\n"
        }
        return words
    }

    fun saveIsMnemonicRemembered() {
        masterSeedRepository.saveIsMnemonicRemembered()
    }
}