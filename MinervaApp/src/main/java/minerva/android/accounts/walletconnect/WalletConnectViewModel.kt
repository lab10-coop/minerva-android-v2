package minerva.android.accounts.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.base.BaseViewModel

class WalletConnectViewModel : BaseViewModel() {

    //TODO get list of connected dapps
    val dapps: List<Dapp> = emptyList()

    private val _viewStateLiveData = MutableLiveData<WalletConnectViewState>()
    val viewStateLiveData: LiveData<WalletConnectViewState> get() = _viewStateLiveData

    fun closeScanner() {
        _viewStateLiveData.value = CloseScannerState
    }
}