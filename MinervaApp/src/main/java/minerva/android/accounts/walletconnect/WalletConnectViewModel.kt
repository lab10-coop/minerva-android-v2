package minerva.android.accounts.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import minerva.android.accounts.transaction.fragment.scanner.AddressParser
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.base.BaseViewModel
import minerva.android.walletConnect.repository.WalletConnectRepository

class WalletConnectViewModel(private val repository: WalletConnectRepository) :
    BaseViewModel() {

    //TODO get list of connected dapps
    val dapps: List<Dapp> = emptyList()

    private val _viewStateLiveData = MutableLiveData<WalletConnectViewState>()
    val viewStateLiveData: LiveData<WalletConnectViewState> get() = _viewStateLiveData

    fun closeScanner() {
        _viewStateLiveData.value = CloseScannerState
    }

    fun handleQrCode(qrCode: String) {
        if (AddressParser.parse(qrCode) != WALLET_CONNECT) {
            _viewStateLiveData.value = WrongQrCodeState
        } else {
            repository.connect(qrCode)

            _viewStateLiveData.value = CorrectQrCodeState
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}