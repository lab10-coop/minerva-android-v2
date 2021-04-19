package minerva.android.settings.fiat

import minerva.android.base.BaseViewModel
import minerva.android.walletmanager.model.Fiat
import minerva.android.walletmanager.storage.LocalStorage

class FiatViewModel(private val localStorage: LocalStorage) : BaseViewModel() {

    fun getCurrentFiatPosition(): Int {
        Fiat.all.forEachIndexed { index, fiat ->
            if (fiat == localStorage.loadCurrentFiat()) return index
        }
        return 0
    }

    fun saveCurrentFiat(fiat: String) = localStorage.saveCurrentFiat(fiat)

}