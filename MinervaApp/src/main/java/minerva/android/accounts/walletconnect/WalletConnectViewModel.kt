package minerva.android.accounts.walletconnect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import minerva.android.accounts.transaction.fragment.scanner.AddressParser
import minerva.android.accounts.transaction.fragment.scanner.AddressParser.WALLET_CONNECT
import minerva.android.base.BaseViewModel
import minerva.android.walletConnect.client.OnConnectionFailure
import minerva.android.walletConnect.client.OnDisconnect
import minerva.android.walletConnect.client.OnSessionRequest
import minerva.android.walletConnect.repository.WalletConnectRepository

class WalletConnectViewModel(private val repository: WalletConnectRepository) :
    BaseViewModel() {

    //TODO get list of connected dapps
    val dapps: List<Dapp> = emptyList()

    private val _viewStateLiveData = MutableLiveData<WalletConnectViewState>()
    val viewStateLiveData: LiveData<WalletConnectViewState> get() = _viewStateLiveData

    init {
        launchDisposable {
            repository.connectionStatusFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        _viewStateLiveData.value = when (it) {
                            is OnSessionRequest -> OnWCSessionRequest(it.meta, it.chainId)
                            is OnConnectionFailure -> OnError(it.error)
                            is OnDisconnect -> OnWCDisconnected(it.reason)
                        }
                    },
                    onError = {
                        _viewStateLiveData.value = OnError(it)
                    }
                )
        }
    }

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