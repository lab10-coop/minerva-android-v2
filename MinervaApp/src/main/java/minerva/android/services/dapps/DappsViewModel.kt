package minerva.android.services.dapps

import androidx.lifecycle.MutableLiveData
import minerva.android.base.BaseViewModel
import minerva.android.services.dapps.model.Dapps

class DappsViewModel : BaseViewModel() {
    private val _dappsLiveData = MutableLiveData(dapps)
    val dappsLiveData get() = _dappsLiveData

    companion object {
        private val dapps = Dapps.values
    }
}